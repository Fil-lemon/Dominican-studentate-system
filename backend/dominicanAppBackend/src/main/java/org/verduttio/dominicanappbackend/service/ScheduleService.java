package org.verduttio.dominicanappbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.verduttio.dominicanappbackend.dto.schedule.AddScheduleForWholePeriodTaskDTO;
import org.verduttio.dominicanappbackend.dto.schedule.ScheduleDTO;
import org.verduttio.dominicanappbackend.dto.user.UserTaskDependencyDTO;
import org.verduttio.dominicanappbackend.entity.*;
import org.verduttio.dominicanappbackend.repository.ScheduleRepository;
import org.verduttio.dominicanappbackend.service.exception.EntityAlreadyExistsException;
import org.verduttio.dominicanappbackend.service.exception.EntityNotFoundException;
import org.verduttio.dominicanappbackend.service.exception.RoleNotMeetRequirementsException;
import org.verduttio.dominicanappbackend.service.exception.ScheduleIsInConflictException;
import org.verduttio.dominicanappbackend.validation.DateValidator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserService userService;
    private final TaskService taskService;
    private final RoleService roleService;
    private final ObstacleService obstacleService;
    private final ConflictService conflictService;

    @Autowired
    public ScheduleService(ScheduleRepository scheduleRepository, UserService userService, TaskService taskService, RoleService roleService, ObstacleService obstacleService, ConflictService conflictService) {
        this.scheduleRepository = scheduleRepository;
        this.userService = userService;
        this.taskService = taskService;
        this.roleService = roleService;
        this.obstacleService = obstacleService;
        this.conflictService = conflictService;
    }

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public Optional<Schedule> getScheduleById(Long scheduleId) {
        return scheduleRepository.findById(scheduleId);
    }

    public void saveSchedule(ScheduleDTO scheduleDTO, boolean ignoreConflicts) {
        validateSchedule(scheduleDTO, ignoreConflicts);

        Schedule schedule = scheduleDTO.toSchedule();
        scheduleRepository.save(schedule);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void createScheduleForWholePeriodTask(AddScheduleForWholePeriodTaskDTO addScheduleDTO, boolean ignoreConflicts) {
        LocalDate from = addScheduleDTO.getFromDate();
        LocalDate to = addScheduleDTO.getToDate();

        if(!DateValidator.dateStartsMondayEndsSunday(from, to)) {
            throw new IllegalArgumentException("Invalid date range. The period must start on Monday and end on Sunday, covering exactly one week.");
        }

        validateAddScheduleForWholePeriodTask(addScheduleDTO, ignoreConflicts, from, to);

        LocalDate date = from;
        while(date.isBefore(to) || date.isEqual(to)) {
            Schedule schedule = new Schedule();
            schedule.setTask(taskService.getTaskById(addScheduleDTO.getTaskId()).get());
            schedule.setUser(userService.getUserById(addScheduleDTO.getUserId()).get());
            schedule.setDate(date);
            scheduleRepository.save(schedule);
            date = date.plusDays(1);
        }

    }

    public void updateSchedule(Long scheduleId, ScheduleDTO updatedScheduleDTO, boolean ignoreConflicts) {
        checkIfScheduleExists(scheduleId);
        validateSchedule(updatedScheduleDTO, ignoreConflicts);

        Schedule schedule = updatedScheduleDTO.toSchedule();
        schedule.setId(scheduleId);
        scheduleRepository.save(schedule);
    }

    public List<Schedule> getSchedulesByUserIdAndDate(Long userId, LocalDate date) {
        return scheduleRepository.findByUserIdAndDate(userId, date);
    }

    public void deleteSchedule(Long scheduleId) {
        checkIfScheduleExists(scheduleId);
        scheduleRepository.deleteById(scheduleId);
    }

    public boolean existsById(Long scheduleId) {
        return scheduleRepository.existsById(scheduleId);
    }

    public List<Schedule> getAllSchedulesByUserId(Long userId) {
        if (!userService.existsById(userId)) {
            throw new EntityNotFoundException("User with given id does not exist");
        }
        return scheduleRepository.findByUserId(userId);
    }

    public List<Schedule> getCurrentSchedules() {
        return scheduleRepository.findSchedulesLaterOrInDay(LocalDate.now());
    }

    public void deleteAllSchedulesByTaskId(Long taskId) {
        scheduleRepository.deleteAllByTaskId(taskId);
    }

    public List<Task> getAvailableTasks(LocalDate from, LocalDate to) {
        List<Task> allTasks = taskService.getAllTasks();
        List<Schedule> schedulesInPeriod = scheduleRepository.findByDateBetween(from, to);

        return getNotFullyAssignedTasks(allTasks, schedulesInPeriod);
    }

    public List<Task> getAvailableTasksBySupervisorRole(String supervisor, LocalDate from, LocalDate to) {
        Role supervisorRole = roleService.findByNameAndType(supervisor, RoleType.SUPERVISOR)
                .orElseThrow(() -> new EntityNotFoundException("Supervisor role not found or not a supervisor"));

        List<Task> allTasks = taskService.findTasksBySupervisorRoleName(supervisorRole.getName());
        List<Schedule> schedulesInPeriod = scheduleRepository.findByDateBetween(from, to);

        return getNotFullyAssignedTasks(allTasks, schedulesInPeriod);
    }

    private List<Task> getNotFullyAssignedTasks(List<Task> allTasks, List<Schedule> schedulesInPeriod) {
        Map<Long, Long> taskOccurrences = schedulesInPeriod.stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getTask().getId(), Collectors.counting()));

        return allTasks.stream().filter(task -> {
            Long occurrences = taskOccurrences.getOrDefault(task.getId(), 0L);
            int requiredOccurrences = task.getParticipantsLimit() * task.getDaysOfWeek().size();
            return occurrences < requiredOccurrences;
        }).collect(Collectors.toList());
    }

    public List<UserTaskDependencyDTO> getAllUserDependenciesForTask(Long taskId, LocalDate from, LocalDate to) {
        List<User> users = userService.getAllUsers();
        return users.stream()
                .map(user -> getUserDependenciesForTask(taskId, user.getId(), from, to))
                .collect(Collectors.toList());
    }

    public UserTaskDependencyDTO getUserDependenciesForTask(Long taskId, Long userId, LocalDate from, LocalDate to) {
        // Sprawdz czy zadanie istnieje
        Task task = taskService.getTaskById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + taskId));

        // Sprawdz czy użytkownik istnieje
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));


        long count = getTaskCompletionCountForUserInLastNDaysFromDate(userId, taskId, from,365);
        LocalDate lastDate = getLastTaskCompletionDateForUser(userId, taskId, from).orElse(null);

        List<Schedule> schedules = getSchedulesByUserIdAndDateBetween(userId, from, to);
        List<String> taskNames = makeUsersTasksInWeekInfoString(schedules);

        // Czy zadany task jest w konflikcie z którymś z tasków pobranych
        List<Task> tasks = getTasksFromSchedules(schedules);
        boolean isConflict = checkIfTaskIsInConflictWithGivenTasks(taskId, tasks);

        // Czy użytkownik posiada aktualną przeszkodę dla zadanego taska
        List<Obstacle> validObstacles = obstacleService.findApprovedObstaclesByUserIdAndTaskIdForDate(userId, taskId, from);
        boolean hasObstacle = !validObstacles.isEmpty();


        return new UserTaskDependencyDTO(userId, user.getName()+" "+user.getSurname(), lastDate, (int) count, taskNames, isConflict, hasObstacle);
    }

    private List<Task> getTasksFromSchedulePerformedByUserAndDateBetween(Long userId, LocalDate from, LocalDate to) {
        List<Schedule> schedules = getSchedulesByUserIdAndDateBetween(userId, from, to);
        return getTasksFromSchedules(schedules);
    }

    public List<String> makeUsersTasksInWeekInfoString(List<Schedule> schedules) {
        // If task appears in the list n times, where n is the task occurrence in the week,
        // then it will be converted to "task.name" only string.
        // If task appears less than n times, then it will be converted to "task.name (P, W, Ś)" string,
        // where P, W, Ś are the days of the week when the task occurs.

        // Possible days of the week
        // Dictionary of DayOfWeek enum and its abbreviation in polish
        Map<DayOfWeek, String> dayOfWeekAbbreviations = new HashMap<>();
        dayOfWeekAbbreviations.put(DayOfWeek.MONDAY, "Pn");
        dayOfWeekAbbreviations.put(DayOfWeek.TUESDAY, "Wt");
        dayOfWeekAbbreviations.put(DayOfWeek.WEDNESDAY, "Śr");
        dayOfWeekAbbreviations.put(DayOfWeek.THURSDAY, "Cz");
        dayOfWeekAbbreviations.put(DayOfWeek.FRIDAY, "Pt");
        dayOfWeekAbbreviations.put(DayOfWeek.SATURDAY, "So");
        dayOfWeekAbbreviations.put(DayOfWeek.SUNDAY, "Nd");

        // Create a map of tasks and their DaysOfWeek assigns from task.date
        // Example: {task: [MONDAY, WEDNESDAY, FRIDAY], task2: [TUESDAY, THURSDAY]}
        Map<Task, Set<DayOfWeek>> taskDaysWhenItIsAssignedInSchedule = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getTask, Collectors.mapping(Schedule::getDate, Collectors.toSet())))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(LocalDate::getDayOfWeek).collect(Collectors.toSet())));



        // Create a list of strings with task names and their occurrences
        List<String> taskInfoStrings = new ArrayList<>();
        for (Map.Entry<Task, Set<DayOfWeek>> entry : taskDaysWhenItIsAssignedInSchedule.entrySet()) {
            Task task = entry.getKey();
            Set<DayOfWeek> occurrences = entry.getValue();
            int requiredOccurrences = task.getDaysOfWeek().size();
            if (occurrences.size() < requiredOccurrences) {
                // If task occurs less than required, then add the days of the week when it occurs
                String daysOfWeekString = occurrences.stream()
                        .sorted()
                        .map(dayOfWeekAbbreviations::get)
                        .collect(Collectors.joining(", "));
                taskInfoStrings.add(task.getName() + " (" + daysOfWeekString + ")");
            } else {
                // If task occurs exactly as required, then add only the task name
                taskInfoStrings.add(task.getName());
            }
        }

        return taskInfoStrings;
    }

    public long getTaskCompletionCountForUserInLastNDaysFromDate(Long userId, Long taskId, LocalDate date, int days) {
        LocalDate startDate = date.minusDays(days);
        // We start counting from the day before the given date
        date = date.minusDays(1);
        return scheduleRepository.countByUserIdAndTaskIdInLastNDays(userId, taskId, startDate, date);
    }

    public Optional<LocalDate> getLastTaskCompletionDateForUser(Long userId, Long taskId, LocalDate upToDate) {
        return scheduleRepository.findLatestTaskCompletionDateByUserIdAndTaskId(userId, taskId, upToDate);
    }

    public List<Schedule> getSchedulesByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to) {
        return scheduleRepository.findByUserIdAndDateBetween(userId, from, to);
    }

    private List<Task> getTasksFromSchedules(List<Schedule> schedules) {
        return schedules.stream().map(Schedule::getTask).collect(Collectors.toList());
    }

    ////////////////////WALIDACJA
    public void validateSchedule(ScheduleDTO scheduleDTO, boolean ignoreConflicts) {
        User user = userService.getUserById(scheduleDTO.getUserId()).orElseThrow(() ->
                new EntityNotFoundException("User with given id does not exist"));

        Task task = taskService.getTaskById(scheduleDTO.getTaskId()).orElseThrow(() ->
                new EntityNotFoundException("Task with given id does not exist"));

        checkIfTaskOccursOnGivenDayOfWeek(scheduleDTO, task);
        checkIfUserHasAllowedRoleForTask(user, task);
        checkIfUserHasValidApprovedObstacleForTask(scheduleDTO.getDate(), user, task);
        checkScheduleConflict(scheduleDTO, ignoreConflicts);
    }

    public void validateAddScheduleForWholePeriodTask(AddScheduleForWholePeriodTaskDTO addScheduleDTO, boolean ignoreConflicts, LocalDate from, LocalDate to) {
        User user = userService.getUserById(addScheduleDTO.getUserId()).orElseThrow(() ->
                new EntityNotFoundException("User with given id does not exist"));

        Task task = taskService.getTaskById(addScheduleDTO.getTaskId()).orElseThrow(() ->
                new EntityNotFoundException("Task with given id does not exist"));

        checkIfUserHasAllowedRoleForTask(user, task);
        checkIfUserHasValidApprovedObstacleForTask(from, user, task);
        List<Schedule> schedules = getSchedulesByUserIdAndDateBetween(addScheduleDTO.getUserId(), from, to);
        List<Task> tasks = getTasksFromSchedules(schedules);
        if(checkIfTaskIsInConflictWithGivenTasks(addScheduleDTO.getTaskId(), tasks) && !ignoreConflicts) {
            throw new ScheduleIsInConflictException("Schedule is in conflict with other schedules");
        }

    }

    public boolean isScheduleInConflictWithOtherSchedules(Schedule schedule) {
        List<Schedule> schedules = scheduleRepository.findByUserIdAndDate(schedule.getUser().getId(), schedule.getDate());
        for(Schedule otherSchedule : schedules) {
            if(conflictService.tasksAreInConflict(schedule.getTask().getId(), otherSchedule.getTask().getId())) {
                return true;
            }
        }
        return false;
    }


    public boolean userHasAllowedRoleForTask(User user, Task task) {
        Set<String> userRoleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        Set<String> allowedRoleNames = task.getAllowedRoles().stream().map(Role::getName).collect(Collectors.toSet());

        return !Collections.disjoint(userRoleNames, allowedRoleNames);
    }

    public void checkIfScheduleExists(Long scheduleId) {
        if(!scheduleRepository.existsById(scheduleId)) {
            throw new EntityNotFoundException("Schedule with given id does not exist");
        }
    }

    public boolean checkIfTaskIsInConflictWithGivenTasks(Long taskId, List<Task> tasks) {
        return tasks.stream().anyMatch(t -> conflictService.tasksAreInConflict(taskId, t.getId()));
    }

    private void checkIfTaskOccursOnGivenDayOfWeek(ScheduleDTO scheduleDTO, Task task) {
        DayOfWeek scheduleDayOfWeek = scheduleDTO.getDate().getDayOfWeek();
        Set<DayOfWeek> taskDaysOfWeek = task.getDaysOfWeek();
        if(!taskDaysOfWeek.contains(scheduleDayOfWeek)) {
            throw new IllegalArgumentException("Task does not occur on given day of week: " + scheduleDayOfWeek);
        }
    }

    private void checkIfUserHasAllowedRoleForTask(User user, Task task) {
        if(!userHasAllowedRoleForTask(user, task)) {
            throw new RoleNotMeetRequirementsException("User does not have allowed role for task");
        }
    }

    private void checkIfUserHasValidApprovedObstacleForTask(LocalDate date, User user, Task task) {
        if(!obstacleService.findApprovedObstaclesByUserIdAndTaskIdForDate(user.getId(), task.getId(), date).isEmpty()) {
            throw new EntityAlreadyExistsException("User has an approved obstacle for this task");
        }
    }

    private void checkScheduleConflict(ScheduleDTO scheduleDTO, boolean ignoreConflicts) {
        if(!ignoreConflicts && isScheduleInConflictWithOtherSchedules(scheduleDTO.toSchedule())) {
            throw new ScheduleIsInConflictException("Schedule is in conflict with other schedules");
        }
    }

}
