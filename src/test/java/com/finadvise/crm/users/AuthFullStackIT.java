package com.finadvise.crm.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AuthFullStackIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void login_Returns200AndToken_OnValidCredentials() throws Exception {
        String rawPassword = "CorrectPassword123!";
        Admin testAdmin = Admin.builder()
                .id(1000L)
                .employeeId("LOGIN-001")
                .firstName("Auth")
                .lastName("Test")
                .phone("987654321")
                .email("auth@test.mail")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .isActive(true)
                .build();
        adminRepository.save(testAdmin);

        LoginRequestDTO request = new LoginRequestDTO("LOGIN-001", rawPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.name").value("Auth Test"))
                .andExpect(jsonPath("$.employeeId").value("LOGIN-001"))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber());
    }

    @Test
    void login_Returns401_OnInvalidCredentials() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("LOGIN-001", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordRecovery_Returns200AndDtoWithAdminEmails() throws Exception {
        String rawPassword = "SomePassword123!";
        Admin testAdmin = Admin.builder()
                .id(2000L)
                .employeeId("REC-001")
                .firstName("Recovery")
                .lastName("Test")
                .phone("987654321")
                .email("recovery@test.mail")
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
                .email("recovery2@test.mail")
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
                .email("inactive@test.mail")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .isActive(false)
                .build();

        adminRepository.save(testAdminInactive);

        mockMvc.perform(get("/api/v1/auth/password-recovery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminEmails", hasSize(greaterThan(0))))
                .andExpect(
                        jsonPath(
                                "$.adminEmails", containsInAnyOrder(
                                        "recovery@test.mail", "recovery2@test.mail", "$INITIAL_ADMIN_EMAIL"
                                )
                        )
                );

    }
}
