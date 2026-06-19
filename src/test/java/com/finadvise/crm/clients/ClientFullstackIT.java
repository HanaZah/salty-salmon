package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressInputDTO;
import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.users.Advisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ClientFullstackIT {

    @Container
    @ServiceConnection
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ClientRepository clientRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;

    private Client testClient;

    private static final LocalDate VALID_BIRTH_DATE = LocalDate.now().minusYears(30);

    @BeforeEach
    void setUp() {
        Advisor primaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                401L, "ADV-0401", "44444441", "PrimaryOwner");

        testClient = testFixtureFactory.getOrCreateTestClient(
                401L, "CLI-0401", "8001014001", "400000001", "Smith", primaryAdvisor);
    }

    // --- GET CLIENT DETAIL TESTS ---

    @Test
    @WithMockUser(username = "ADV-0401", authorities = "ADVISOR")
    void getClientDetail_Success_Returns200AndClientDto() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientUid}", testClient.getClientUid())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientUid").value(testClient.getClientUid()))
                .andExpect(jsonPath("$.personalId").value(testClient.getPersonalId()))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    @WithMockUser(username = "ROG-0402", authorities = "ADVISOR")
    void getClientDetail_Fails_ReturnsOpaque404_WhenNotAssignedAdvisor() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientUid}", testClient.getClientUid())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Client not found or access denied"));
    }

    // --- CREATE CLIENT TESTS ---

    @Test
    @WithMockUser(username = "ADV-0401", authorities = "ADVISOR")
    void createClient_Success_Returns201AndCreatedDto() throws Exception {
        AddressInputDTO validAddress = new AddressInputDTO("Main Street", "123", "Prague", "110 00");
        ClientCreateRequestDTO payload = new ClientCreateRequestDTO(
                "9001011234", VALID_BIRTH_DATE, "John", "Doe", "Developer", "+420123456789", "john@test.com",
                "123456789", LocalDate.now().minusDays(10), LocalDate.now().plusYears(10), "Ministry",
                validAddress, validAddress
        );

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientUid").exists())
                .andExpect(jsonPath("$.personalId").value("9001011234"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser(username = "ADV-0401", authorities = "ADVISOR")
    void createClient_Fails_Returns409_WhenPersonalIdExists() throws Exception {
        AddressInputDTO validAddress = new AddressInputDTO("Main Street", "123", "Prague", "110 00");
        ClientCreateRequestDTO payload = new ClientCreateRequestDTO(
                testClient.getPersonalId(), VALID_BIRTH_DATE, "Jane", "Doe", "Developer", "+420123456789", "jane@test.com",
                "987654321", LocalDate.now().minusDays(10), LocalDate.now().plusYears(10), "Ministry",
                validAddress, validAddress
        );

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail").value("Client with this personal ID already exists."));
    }

    @Test
    @WithMockUser(username = "ADV-0401", authorities = "ADVISOR")
    void createClient_Fails_Returns400_WhenValidationFails() throws Exception {
        AddressInputDTO validAddress = new AddressInputDTO("Main Street", "123", "Prague", "110 00");
        ClientCreateRequestDTO invalidPayload = new ClientCreateRequestDTO(
                "", VALID_BIRTH_DATE, "", "Doe", "Developer", "+420123456789", "invalid-email",
                "123456789", LocalDate.now().minusDays(10), LocalDate.now().plusYears(10), "Ministry",
                validAddress, validAddress
        );

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").exists());
    }

    // --- UPDATE DETAILS TESTS ---

    @Test
    @WithMockUser(username = "ADV-0401", authorities = "ADVISOR")
    void updateClientDetails_Success_Returns204NoContent() throws Exception {
        AddressInputDTO permAddress = new AddressInputDTO("Updated Street", "456", "Brno", "602 00");
        AddressInputDTO contactAddress = new AddressInputDTO("Contact Street", "789", "Ostrava", "702 00");

        ClientUpdateDetailsRequestDTO payload = new ClientUpdateDetailsRequestDTO(
                "UpdatedName", "UpdatedLastName", "UpdatedOccupation", "+420987654321", "updated@test.com",
                permAddress, contactAddress
        );

        mockMvc.perform(put("/api/v1/clients/{clientUid}/details", testClient.getClientUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNoContent());

        Client updatedClient = clientRepository.findByClientUidAndIsActiveTrue(testClient.getClientUid()).orElseThrow();
        assertThat(updatedClient.getFirstName()).isEqualTo("UpdatedName");
    }

    @Test
    @WithMockUser(username = "ROG-0402", authorities = "ADVISOR")
    void updateClientDetails_ReturnsOpaque404_ToPreventIdEnumeration() throws Exception {
        AddressInputDTO validAddress = new AddressInputDTO("Main Street", "123", "Prague", "110 00");
        ClientUpdateDetailsRequestDTO payload = new ClientUpdateDetailsRequestDTO(
                "HackedName", "HackedLastName", "Hacker", "+420000000000", "hack@test.com",
                validAddress, validAddress
        );

        mockMvc.perform(put("/api/v1/clients/{clientUid}/details", testClient.getClientUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    // --- UPDATE ID CARD TESTS ---

    @Test
    @WithMockUser(username = "ADV-0401", authorities = "ADVISOR")
    void updateClientIdCard_Success_Returns204NoContent() throws Exception {
        ClientUpdateIdCardRequestDTO payload = new ClientUpdateIdCardRequestDTO(
                "999888777", LocalDate.now().minusYears(1), LocalDate.now().plusYears(9), "New City Hall"
        );

        mockMvc.perform(put("/api/v1/clients/{clientUid}/id-card", testClient.getClientUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNoContent());

        Client updatedClient = clientRepository.findByClientUidAndIsActiveTrue(testClient.getClientUid()).orElseThrow();
        assertThat(updatedClient.getIdCardNumber()).isEqualTo("999888777");
    }

    // --- DELETE CLIENT TESTS ---

    @Test
    @WithMockUser(username = "ADV-0401", authorities = "ADVISOR")
    void deleteClient_Success_Returns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/{clientUid}", testClient.getClientUid()))
                .andExpect(status().isNoContent());

        assertThat(clientRepository.findByClientUidAndIsActiveTrue(testClient.getClientUid())).isEmpty();
    }

    @Test
    @WithMockUser(username = "ADMIN-0403", authorities = "ADMIN")
    void deleteClient_SuccessAsAdmin_Returns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/{clientUid}", testClient.getClientUid()))
                .andExpect(status().isNoContent());

        assertThat(clientRepository.findByClientUidAndIsActiveTrue(testClient.getClientUid())).isEmpty();
    }

    @Test
    @WithMockUser(username = "ROG-0402", authorities = "ADVISOR")
    void deleteClient_ReturnsOpaque404_ToPreventIdEnumeration() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/{clientUid}", testClient.getClientUid()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    // --- SEARCH CLIENTS TESTS ---

    @Test
    @WithMockUser(username = "ADV-0401", authorities = "ADVISOR")
    void searchClients_Success_ReturnsPaginatedResults() throws Exception {
        ClientSearchCriteriaDTO criteria = new ClientSearchCriteriaDTO(
                null, "Smith", null, null, null, null, null, null, null, null, null, null
        );

        // Force Hibernate to write the @BeforeEach testClient to the DB
        // so the V_CLIENT_SEARCH_MINIMAL view can actually see it.
        clientRepository.flush();

        mockMvc.perform(post("/api/v1/clients/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criteria))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].clientUid").value(testClient.getClientUid()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- DASHBOARD SUMMARIES TESTS ---

    @Test
    @WithMockUser(username = "ADV-0401", authorities = "ADVISOR")
    void getRecentClientSummaries_Success_Returns200AndDashboardPage() throws Exception {
        mockMvc.perform(get("/api/v1/clients/dashboard")
                        .param("pageSize", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].clientUid").value(testClient.getClientUid()));
    }
}
