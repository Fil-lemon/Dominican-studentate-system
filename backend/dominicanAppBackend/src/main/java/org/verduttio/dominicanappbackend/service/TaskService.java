package org.verduttio.dominicanappbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.verduttio.dominicanappbackend.dto.TaskDTO;
import org.verduttio.dominicanappbackend.entity.Role;
import org.verduttio.dominicanappbackend.entity.Task;
import org.verduttio.dominicanappbackend.repository.TaskRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final RoleService roleService;

    @Autowired
    public TaskService(TaskRepository taskRepository, RoleService roleService) {
        this.taskRepository = taskRepository;
        this.roleService = roleService;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(Long taskId) {
        return taskRepository.findById(taskId);
    }

    public void saveTask(TaskDTO taskDTO) {
        Task task = convertTaskDTOToTask(taskDTO);
        taskRepository.save(task);
    }

    public void saveTask(Task task) {
        taskRepository.save(task);
    }

    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    private Task convertTaskDTOToTask(TaskDTO taskDTO) {
        Task task = taskDTO.basicFieldsToTask();
        Set<Role> rolesDB = roleService.getRolesByRoleNames(taskDTO.getAllowedRoleNames());
        task.setAllowedRoles(rolesDB);

        return task;
    }

    public void updateTask(Task task, TaskDTO updatedTaskDTO) {
        task.setName(updatedTaskDTO.getName());
        task.setCategory(updatedTaskDTO.getCategory());
        task.setParticipantsLimit(updatedTaskDTO.getParticipantsLimit());
        task.setPermanent(updatedTaskDTO.isPermanent());
        task.setParticipantForWholePeriod(updatedTaskDTO.isParticipantForWholePeriod());
        Set<Role> rolesDB = roleService.getRolesByRoleNames(updatedTaskDTO.getAllowedRoleNames());
        task.setAllowedRoles(rolesDB);
        task.setDaysOfWeek(updatedTaskDTO.getDaysOfWeek());

        taskRepository.save(task);
    }
}
