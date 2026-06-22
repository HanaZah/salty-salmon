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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class UserFullStackIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestFixtureFactory testFixtureFactory;
    @Autowired private AdvisorRepository advisorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminRepository adminRepository;

    // --- ME E2E ---

    @Test
    @WithMockUser(username = "ME-0102")
    void getCurrentUser_ReturnsProfile_BasedOnPrincipal() throws Exception {
        testFixtureFactory.getOrCreateTestAdvisor(102L, "ME-0102", "10201020", "MeName");

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("MeName"));
    }

    // --- PROFILE & PASSWORD E2E ---

    @Test
    @WithMockUser(username = "UPD-0107", authorities = "ADVISOR")
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
    @WithMockUser(username = "PWD-0108", authorities = "ADVISOR")
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
    @WithMockUser(username = "BAD-0109", authorities = "ADVISOR")
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

    // --- ADMIN CONTACTS E2E ---

    @Test
    @WithMockUser(authorities = "ADVISOR")
    void getAllAdminsContacts_Returns200AndListOfActiveAdminsContacts() throws Exception {
        String rawPassword = "SomePassword123!";
        Admin testAdmin = Admin.builder()
                .id(2000L)
                .employeeId("REC-001")
                .firstName("Recovery")
                .lastName("Test")
                .phone("987654321")
                .email("recovery@test.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .isActive(true)
                .build();
        adminRepository.save(testAdmin);

        Admin testAdmin2 = Admin.builder()
                .id(3000L)
                .employeeId("REC-002")
                .firstName("Recovery2")
                .lastName("Test")
                .phone("987654321")
                .email("recovery2@test.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .isActive(true)
                .build();
        adminRepository.save(testAdmin2);

        Admin testAdminInactive = Admin.builder()
                .id(4000L)
                .employeeId("REC-003")
                .firstName("Inactive")
                .lastName("Test")
                .phone("987654321")
                .email("inactive@test.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .isActive(false)
                .build();

        adminRepository.save(testAdminInactive);

        mockMvc.perform(get("/api/v1/users/admins/contacts"))
                .andExpect(status().isOk())
                // Admin db seeder may or may not be present, so we check for at least 2 admins (there could be more)
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[*].email", not(hasItem(testAdminInactive.getEmail()))));
    }
}