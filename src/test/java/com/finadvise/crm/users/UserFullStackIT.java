package com.finadvise.crm.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserFullStackIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdvisorRepository advisorRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createNewAdvisor_Returns401_WhenUnauthenticated() throws Exception {
        CreateAdvisorRequestDTO request = new CreateAdvisorRequestDTO(
                "John", "Doe", "12345678", "john@finadvise.com", "1234567890", "Pass123"
        );

        mockMvc.perform(post("/api/v1/users/new/advisor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // No .with(jwt()) attached
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createNewAdvisor_Returns403_WhenUserIsNotAdmin() throws Exception {
        CreateAdvisorRequestDTO request = new CreateAdvisorRequestDTO(
                "Jane", "Doe", "87654321", "jane@finadvise.com", "0987654321", "Pass123"
        );

        mockMvc.perform(post("/api/v1/users/new/advisor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        // Simulating a valid token, but with the ADVISOR role instead of ADMIN
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "ROLE_ADVISOR"))))
                .andExpect(status().isForbidden());
    }


    @Test
    void createNewAdvisor_Returns200AndDto_WhenUserIsAdmin() throws Exception {
        CreateAdvisorRequestDTO request = new CreateAdvisorRequestDTO(
                "Alice", "Smith", "11223344", "alice@finadvise.com", "1112223333", "Pass123"
        );

        // Recreates the logic of our production converter just for the test mutator
        JwtGrantedAuthoritiesConverter testConverter = new JwtGrantedAuthoritiesConverter();
        testConverter.setAuthoritiesClaimName("scope");
        testConverter.setAuthorityPrefix("");

        mockMvc.perform(post("/api/v1/users/new/advisor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        // Simulating a valid token with the required ADMIN role
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("scope", "ROLE_ADMIN"))
                                .authorities(testConverter)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.ico").value("11223344"))
                .andExpect(jsonPath("$.employeeId").exists());
    }

    @Test
    void getAdvisorById_Returns404_WhenAdvisorDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/users/99999")
                        // Assuming any authenticated user can attempt to read a profile
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "ROLE_ADVISOR"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_Returns204_OnValidRequest() throws Exception {
        // We need an existing user in the TestContainer database to update
        Advisor advisor = Advisor.builder()
                .id(500L)
                .employeeId("UPDATE-001")
                .passwordHash("hash")
                .firstName("Original")
                .lastName("Name")
                .ico("12345678")
                .email("update@finadvise.com")
                .phone("0000000000")
                .isActive(true)
                .build();

        // Save it directly using your repository (or testEntityManager if autowired here)
        advisorRepository.save(advisor);

        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO("UpdatedFirst", "UpdatedLast", "1112223333");

        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        // Mock the JWT with the exact subject matching our saved entity
                        .with(jwt().jwt(jwt -> jwt.subject("UPDATE-001").claim("scope", "ROLE_ADVISOR"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateProfile_Returns401_WhenUnauthenticated() throws Exception {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO("Hacker", "Man", "9998887777");

        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_Returns204_OnValidRequest() throws Exception {
        // We use the real password encoder to generate a valid hash for the TestContainer
        String currentHashedPassword = passwordEncoder.encode("CurrentPass123");

        Admin admin = Admin.builder()
                .id(600L)
                .employeeId("PASS-001")
                .passwordHash(currentHashedPassword)
                .firstName("Pass")
                .lastName("Changer")
                .isActive(true)
                .build();

        adminRepository.save(admin);

        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("CurrentPass123", "BrandNewPass456");

        mockMvc.perform(patch("/api/v1/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(jwt -> jwt.subject("PASS-001").claim("scope", "ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePassword_Returns400_WhenOldPasswordIsWrong() throws Exception {
        String currentHashedPassword = passwordEncoder.encode("ActualPass123");
        Admin admin = Admin.builder()
                .id(700L)
                .employeeId("BAD-PASS-001")
                .passwordHash(currentHashedPassword)
                .firstName("Bad")
                .lastName("Passer")
                .isActive(true)
                .build();
        adminRepository.save(admin);

        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("WrongGuess999", "NewHackedPass123");

        mockMvc.perform(patch("/api/v1/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(jwt -> jwt.subject("BAD-PASS-001")))) // Authenticated as the actual user
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Incorrect current password."));
    }

    @Test
    void changePassword_Returns400_WhenNewPasswordIsSameAsOld() throws Exception {
        String currentHashedPassword = passwordEncoder.encode("StubbornPass123");
        Admin admin = Admin.builder()
                .id(800L)
                .employeeId("STUBBORN-001")
                .passwordHash(currentHashedPassword)
                .firstName("Stubborn")
                .lastName("User")
                .isActive(true)
                .build();
        adminRepository.save(admin);

        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("StubbornPass123", "StubbornPass123");

        mockMvc.perform(patch("/api/v1/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(jwt -> jwt.subject("STUBBORN-001"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("New password cannot be the same as the old password."));
    }
}
