package org.verduttio.dominicanappbackend.unittest.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.verduttio.dominicanappbackend.entity.Schedule;
import org.verduttio.dominicanappbackend.entity.Task;
import org.verduttio.dominicanappbackend.service.ScheduleService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ScheduleServiceTest {
    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void makeUsersTasksInWeekInfoString_mixOfAllAndPartAssignTasks() {
        Task task1 = new Task("Washing", "1",3, true, true, null, null,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
        Task task2 = new Task("Cooking", "1",2, true, true, null, null,
                Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));
        Task task3 = new Task("Drying", "1",2, true, true, null, null,
                Set.of(DayOfWeek.THURSDAY, DayOfWeek.SATURDAY));

        Schedule schedule1 = new Schedule();
        schedule1.setTask(task1);
        schedule1.setDate(LocalDate.parse("2024-02-05"));

        Schedule schedule2 = new Schedule();
        schedule2.setTask(task3);
        schedule2.setDate(LocalDate.parse("2024-02-10"));

        Schedule schedule3 = new Schedule();
        schedule3.setTask(task1);
        schedule3.setDate(LocalDate.parse("2024-02-09"));

        Schedule schedule4 = new Schedule();
        schedule4.setTask(task2);
        schedule4.setDate(LocalDate.parse("2024-02-06"));

        Schedule schedule5 = new Schedule();
        schedule5.setTask(task2);
        schedule5.setDate(LocalDate.parse("2024-02-08"));

        List<Schedule> schedules = List.of(schedule1, schedule2, schedule3, schedule4, schedule5);

        List<String> testResult = scheduleService.createInfoStringsOfTasksOccurrenceFromGivenSchedule(schedules);
        List<String> expectedResult = List.of("Cooking", "Drying (So)", "Washing (Pn, Pt)");

        assertEquals(expectedResult, testResult);
    }
}
