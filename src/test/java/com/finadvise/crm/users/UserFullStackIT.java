package com.finadvise.crm.users;

import com.finadvise.crm.common.TestFixtureFactory;
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
@Transactional // Rolls back the database after each test, keeping our fixtures isolated
class UserFullStackIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestFixtureFactory testFixtureFactory;
    @Autowired private AdvisorRepository advisorRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // --- CREATE ADMIN / ADVISOR E2E ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void createNewAdmin_Returns200AndDto_WhenUserIsAdmin() throws Exception {
        CreateAdminRequestDTO request = new CreateAdminRequestDTO("Bob", "Builder", "bob@builder.com", "SecurePass1!");

        mockMvc.perform(post("/api/v1/users/new/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Bob"))
                .andExpect(jsonPath("$.employeeId").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createNewAdmin_Returns409_WhenEmailExists() throws Exception {
        // Use factory to create a valid baseline, then modify the email to trigger the conflict
        Advisor existingUser = testFixtureFactory.getOrCreateTestAdvisor(101L, "EMP-0101", "10101010", "Conflict");
        existingUser.setEmail("conflict@mail.com");
        advisorRepository.save(existingUser);

        CreateAdminRequestDTO request = new CreateAdminRequestDTO("Test", "User", "conflict@mail.com", "Pass!");

        mockMvc.perform(post("/api/v1/users/new/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createNewAdvisor_Returns200AndDto_WhenUserIsAdmin() throws Exception {
        CreateAdvisorRequestDTO request = new CreateAdvisorRequestDTO(
                "Alice", "Smith", "11223344", "alice@finadvise.com", "1112223333", "Pass123"
        );

        mockMvc.perform(post("/api/v1/users/new/advisor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.ico").value("11223344"));
    }

    // --- ME & LISTING E2E ---

    @Test
    @WithMockUser(username = "ME-0102")
    void getCurrentUser_ReturnsProfile_BasedOnPrincipal() throws Exception {
        testFixtureFactory.getOrCreateTestAdvisor(102L, "ME-0102", "10201020", "MeName");

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("MeName"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAdvisors_ReturnsPagedList_ForAdmin() throws Exception {
        testFixtureFactory.getOrCreateTestAdvisor(103L, "LST-0103", "10301030", "ListName");

        mockMvc.perform(get("/api/v1/users/advisors")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    // --- ASSIGN & DEACTIVATE E2E ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignManager_Returns204_WhenSuccessful() throws Exception {
        testFixtureFactory.getOrCreateTestAdvisor(104L, "MGR-0104", "10401040", "Manager");
        testFixtureFactory.getOrCreateTestAdvisor(105L, "EMP-0105", "10501050", "Employee");

        AssignManagerRequestDTO request = new AssignManagerRequestDTO("MGR-0104");

        mockMvc.perform(patch("/api/v1/users/{employeeId}/manager", "EMP-0105")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateUser_Returns204_WhenSuccessful() throws Exception {
        testFixtureFactory.getOrCreateTestAdvisor(106L, "TGT-0106", "10601060", "Target");

        mockMvc.perform(delete("/api/v1/users/{employeeId}", "TGT-0106"))
                .andExpect(status().isNoContent());
    }

    // --- PROFILE & PASSWORD E2E ---

    @Test
    @WithMockUser(username = "UPD-0107", roles = "ADVISOR")
    void updateProfile_Returns204_OnValidRequest() throws Exception {
        Advisor advisor = testFixtureFactory.getOrCreateTestAdvisor(107L, "UPD-0107", "10701070", "Update");

        // Factory sets version to 0 natively
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(advisor.getVersion(), "UpdatedFirst", "UpdatedLast", "1112223333");

        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "PWD-0108", roles = "ADVISOR")
    void changePassword_Returns204_OnValidRequest() throws Exception {
        Advisor advisor = testFixtureFactory.getOrCreateTestAdvisor(108L, "PWD-0108", "10801080", "PassChanger");
        advisor.setPasswordHash(passwordEncoder.encode("CurrentPass123"));
        advisorRepository.save(advisor);

        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("CurrentPass123", "BrandNewPass456");

        mockMvc.perform(patch("/api/v1/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "BAD-0109", roles = "ADVISOR")
    void changePassword_Returns400_WhenOldPasswordIsWrong() throws Exception {
        Advisor advisor = testFixtureFactory.getOrCreateTestAdvisor(109L, "BAD-0109", "10901090", "BadPasser");
        advisor.setPasswordHash(passwordEncoder.encode("ActualPass123"));
        advisorRepository.save(advisor);

        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("WrongGuess999", "NewHackedPass123");

        mockMvc.perform(patch("/api/v1/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Incorrect current password."));
    }
}