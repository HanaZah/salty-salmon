package com.finadvise.crm.budget;

import com.finadvise.crm.clients.Client;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class BudgetFullstackIT {
    @Container
    @ServiceConnection
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private IncomeRepository incomeRepository;
    @Autowired private IncomeTypeRepository incomeTypeRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;

    private Client testClient;

    @BeforeEach
    void setUp() { Advisor advisor = testFixtureFactory.getOrCreateTestAdvisor(1234L, "ADV-1234", "12312312", "Fullbudget");

        testClient = testFixtureFactory.getOrCreateTestClient(
                11L, "CLI_11" ,"0987654321", "987654321", "Smith", advisor);

        if (!incomeTypeRepository.existsByName("Zaměstnání")) {
            incomeTypeRepository.save(IncomeType.builder().name("Zaměstnání").build());
        }
    }

    @Test
    @WithMockUser(username = "ADV-1234", roles = "ADVISOR")
    void updateBudget_Success_CreatesNewIncome() throws Exception {
        // 1. Arrange: Create a payload simulating the UI adding a new income
        BudgetItemDTO newIncomeDto = new BudgetItemDTO(
                null, // null ID means create
                "Zaměstnání",
                50000,
                null,
                null // null version for new items
        );

        BudgetFullDTO payload = new BudgetFullDTO(
                null, null, null,
                List.of(newIncomeDto),
                List.of()
        );

        // 2. Act: Perform the PUT request
        mockMvc.perform(put("/api/v1/clients/{clientId}/budget", testClient.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNoContent()); // 3. Assert HTTP Status

        // 4. Assert Database State
        List<Income> savedIncomes = incomeRepository.findAllByClientId(testClient.getId());
        assertThat(savedIncomes).hasSize(1);
        assertThat(savedIncomes.get(0).getAmount()).isEqualTo(50000);
        assertThat(savedIncomes.get(0).getIncomeType().getName()).isEqualTo("Zaměstnání");
    }

    @Test
    @WithMockUser(username = "ROGUE_99", roles = "ADVISOR")
    void updateBudget_Fails_WhenAdvisorDoesNotOwnClient() throws Exception {
        // 1. Arrange
        BudgetFullDTO payload = new BudgetFullDTO(null, null, null, List.of(), List.of());

        // 2. Act & Assert: Expect 403 Forbidden due to OwnershipValidator
        mockMvc.perform(put("/api/v1/clients/{clientId}/budget", testClient.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access Denied"))
                .andExpect(jsonPath("$.detail").value("Unauthorized budget update attempt."));
    }
}
