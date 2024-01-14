package org.verduttio.dominicanappbackend.integrationtest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.verduttio.dominicanappbackend.entity.*;
import org.verduttio.dominicanappbackend.integrationtest.utility.DatabaseInitializer;
import org.verduttio.dominicanappbackend.repository.ObstacleRepository;
import org.verduttio.dominicanappbackend.repository.RoleRepository;
import org.verduttio.dominicanappbackend.repository.TaskRepository;
import org.verduttio.dominicanappbackend.repository.UserRepository;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration_tests")
public class ObstacleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObstacleRepository obstacleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @Test
    public void getAllObstacles_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/obstacles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(0))));
    }

    @Test
    public void getObstacleById_WithExistingId_ShouldReturnOk() throws Exception {
        Role role = databaseInitializer.addRoleUser();
        User frankCadillac = databaseInitializer.addUserFrankCadillac(Set.of(role));
        Task task = databaseInitializer.addDryDishesTask(Set.of(role));
        Obstacle obstacle = databaseInitializer.addObstacle_01_01_To_01_20(frankCadillac, task);

        mockMvc.perform(get("/api/obstacles/" + obstacle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(obstacle.getId().intValue())));

        databaseInitializer.clearDb();
    }

    @Test
    public void getObstacleById_WithNonExistingId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/obstacles/9999"))
                .andExpect(status().isNotFound());
    }

    // POST /api/obstacles
    @Test
    public void createObstacle_WithValidData_ShouldReturnCreated() throws Exception {
        Role role = databaseInitializer.addRoleUser();
        User frankCadillac = databaseInitializer.addUserFrankCadillac(Set.of(role));
        Task task = databaseInitializer.addDryDishesTask(Set.of(role));


        String obstacleJson = "{\"userId\":"+frankCadillac.getId()+",\"taskId\":"+task.getId()+",\"fromDate\":\"2024-01-01\",\"toDate\":\"2024-01-02\",\"applicantDescription\":\"Test Description\"}";

        mockMvc.perform(post("/api/obstacles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(obstacleJson))
                .andExpect(status().isCreated());

        List<Obstacle> obstacles = obstacleRepository.findAll();
        assertTrue(obstacles.stream().anyMatch(o -> "Test Description".equals(o.getApplicantDescription())));

        databaseInitializer.clearDb();
    }

    // PATCH /api/obstacles/{obstacleId}
    @Test
    public void updateObstacle_WithExistingId_ShouldReturnOk() throws Exception {
        Role role = databaseInitializer.addRoleUser();
        User frankCadillac = databaseInitializer.addUserFrankCadillac(Set.of(role));
        Task task = databaseInitializer.addDryDishesTask(Set.of(role));
        Obstacle obstacle = databaseInitializer.addObstacle_01_01_To_01_20(frankCadillac, task);
        User johnDoe = databaseInitializer.addUserJohnDoe(Set.of(role));

        String updatedObstacleJson = "{\"status\":\"APPROVED\",\"recipientAnswer\":\"Approved\",\"recipientUserId\":"+johnDoe.getId()+"}";

        mockMvc.perform(patch("/api/obstacles/" + obstacle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedObstacleJson))
                .andExpect(status().isOk());

        Obstacle updatedObstacle = obstacleRepository.findById(obstacle.getId()).orElse(null);
        assertNotNull(updatedObstacle);
        assertEquals(ObstacleStatus.APPROVED, updatedObstacle.getStatus());

        databaseInitializer.clearDb();
    }

    // DELETE /api/obstacles/{obstacleId}
    @Test
    public void deleteObstacle_WithExistingId_ShouldReturnNoContent() throws Exception {
        Role role = databaseInitializer.addRoleUser();
        User frankCadillac = databaseInitializer.addUserFrankCadillac(Set.of(role));
        Task task = databaseInitializer.addDryDishesTask(Set.of(role));
        Obstacle obstacle = databaseInitializer.addObstacle_01_01_To_01_20(frankCadillac, task);

        mockMvc.perform(delete("/api/obstacles/" + obstacle.getId()))
                .andExpect(status().isNoContent());

        boolean exists = obstacleRepository.existsById(obstacle.getId());
        assertFalse(exists);

        databaseInitializer.clearDb();
    }
}
