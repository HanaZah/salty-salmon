package com.finadvise.crm.administration;

import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class UserAdministrationFullstackIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestFixtureFactory testFixtureFactory;
    @Autowired private AdvisorRepository advisorRepository;

    // --- CREATE ADMIN / ADVISOR E2E ---

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createNewAdmin_Returns200AndDto_WhenUserIsAdmin() throws Exception {
        CreateAdminRequestDTO request = new CreateAdminRequestDTO(
                "Bob", "Builder", "bob@builder.com", "123456789", "SecurePass1!"
        );

        mockMvc.perform(post("/api/v1/administration/users/new/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Bob"))
                .andExpect(jsonPath("$.employeeId").exists());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createNewAdmin_Returns409_WhenEmailExists() throws Exception {
        // Use factory to create a valid baseline, then modify the email to trigger the conflict
        Advisor existingUser = testFixtureFactory.getOrCreateTestAdvisor(101L, "EMP-0101", "10101010", "Conflict");
        existingUser.setEmail("conflict@mail.com");
        advisorRepository.save(existingUser);

        CreateAdminRequestDTO request = new CreateAdminRequestDTO(
                "Test", "User", "conflict@mail.com", "123456789","PassWord!"
        );

        mockMvc.perform(post("/api/v1/administration/users/new/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createNewAdvisor_Returns200AndDto_WhenUserIsAdmin() throws Exception {
        CreateAdvisorRequestDTO request = new CreateAdvisorRequestDTO(
                "Alice", "Smith", "11223344", "alice@finadvise.com", "1112223333", "Pass123!"
        );

        mockMvc.perform(post("/api/v1/administration/users/new/advisor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.ico").value("11223344"));
    }

    // --- ASSIGN & DEACTIVATE E2E ---

    @Test
    @WithMockUser(authorities = "ADMIN")
    void assignManager_Returns204_WhenSuccessful() throws Exception {
        testFixtureFactory.getOrCreateTestAdvisor(104L, "MGR-0104", "10401040", "Manager");
        testFixtureFactory.getOrCreateTestAdvisor(105L, "EMP-0105", "10501050", "Employee");

        AssignManagerRequestDTO request = new AssignManagerRequestDTO("MGR-0104");

        mockMvc.perform(patch("/api/v1/administration/users/{employeeId}/manager", "EMP-0105")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deactivateUser_Returns204_WhenSuccessful() throws Exception {
        testFixtureFactory.getOrCreateTestAdvisor(106L, "TGT-0106", "10601060", "Target");

        mockMvc.perform(delete("/api/v1/administration/users/{employeeId}", "TGT-0106"))
                .andExpect(status().isNoContent());
    }

    // --- GET ALL ADMINS & ADVISORS E2E ---

    @Test
    @WithMockUser(authorities = "ADMIN")
    void searchAdvisors_ReturnsPagedList_ForAdmin() throws Exception {
        String uniqueLastName = "SearchableLastName123";
        testFixtureFactory.getOrCreateTestAdvisor(103L, "LST-0103", "10301030", uniqueLastName);

        mockMvc.perform(get("/api/v1/administration/users/advisors")
                        .param("lastName", uniqueLastName)
                        .param("isActive", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].lastName").value(uniqueLastName))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void searchAdvisors_ReturnsEmptyPage_WhenNoMatchFound() throws Exception {
        String nonExistentIco = "99999999";

        mockMvc.perform(get("/api/v1/administration/users/advisors")
                        .param("ico", nonExistentIco)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
