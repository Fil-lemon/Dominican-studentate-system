package org.verduttio.dominicanappbackend.unittest.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.verduttio.dominicanappbackend.domain.Role;
import org.verduttio.dominicanappbackend.domain.RoleType;
import org.verduttio.dominicanappbackend.domain.Task;
import org.verduttio.dominicanappbackend.repository.RoleRepository;
import org.verduttio.dominicanappbackend.repository.TaskRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("integration_tests")
public class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void testFindAllByOrderBySortOrderAsc() {
        // Given
        Role superVisorRole = new Role("Super", RoleType.SUPERVISOR);
        superVisorRole.setSortOrder(1L);
        roleRepository.save(superVisorRole);

        Task task1 = new Task();
        task1.setName("Task 1");
        task1.setSortOrder(2L);
        task1.setSupervisorRole(superVisorRole);
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setName("Task 2");
        task2.setSortOrder(1L);
        task2.setSupervisorRole(superVisorRole);
        taskRepository.save(task2);

        Task task3 = new Task();
        task3.setName("Task 3");
        task3.setSortOrder(3L);
        task3.setSupervisorRole(superVisorRole);
        taskRepository.save(task3);

        // When
        List<Task> tasks = taskRepository.findAllTasksOrderBySupervisorRoleSortOrderAndTaskSortOrder();

        // Then
        assertEquals(3, tasks.size(), "All tasks should be returned");
        assertEquals("Task 2", tasks.get(0).getName(), "First should be: 'Task 2'");
        assertEquals("Task 1", tasks.get(1).getName(), "Second should be: 'Task 1'");
        assertEquals("Task 3", tasks.get(2).getName(), "Third should be: 'Task 3'");
    }
}
