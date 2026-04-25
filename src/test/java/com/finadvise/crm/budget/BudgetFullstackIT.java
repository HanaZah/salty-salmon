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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private ExpenseTypeRepository expenseTypeRepository;

    private Client testClient;

    @BeforeEach
    void setUp() { Advisor advisor = testFixtureFactory.getOrCreateTestAdvisor(1234L, "ADV_1234", "12312312", "Fullbudget");

        testClient = testFixtureFactory.getOrCreateTestClient(
                11L, "CLI_11" ,"0987654321", "987654321", "Smith", advisor);

        if (!incomeTypeRepository.existsByName("Zaměstnání")) {
            incomeTypeRepository.save(IncomeType.builder().name("Zaměstnání").build());
        }
    }

    @Test
    @WithMockUser(username = "ADV_1234", roles = "ADVISOR")
    void updateBudget_Success_CreatesNewIncome() throws Exception {
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

        mockMvc.perform(put("/api/v1/clients/{clientId}/budget", testClient.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNoContent());

        List<Income> savedIncomes = incomeRepository.findAllByClientId(testClient.getId());
        assertThat(savedIncomes).hasSize(1);
        assertThat(savedIncomes.getFirst().getAmount()).isEqualTo(50000);
        assertThat(savedIncomes.getFirst().getIncomeType().getName()).isEqualTo("Zaměstnání");
    }

    @Test
    @WithMockUser(username = "ROGUE_99", roles = "ADVISOR")
    void updateBudget_Fails_WhenAdvisorDoesNotOwnClient() throws Exception {
        BudgetFullDTO payload = new BudgetFullDTO(null, null, null, List.of(), List.of());

        mockMvc.perform(put("/api/v1/clients/{clientId}/budget", testClient.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access Denied"))
                .andExpect(jsonPath("$.detail").value("Unauthorized budget update attempt."));
    }

    @Test
    @WithMockUser(username = "ADV_1234", roles = "ADVISOR")
    void getBudget_Success_ReturnsCalculatedBudgetSnapshot() throws Exception {
        IncomeType salaryType = incomeTypeRepository.findByName("Zaměstnání").orElseThrow();
        incomeRepository.save(Income.builder()
                .amount(50000)
                .incomeType(salaryType)
                .client(testClient)
                .build());

        ExpenseType rentType = expenseTypeRepository.findByName("Nájem")
                .orElseGet(() -> expenseTypeRepository.save(ExpenseType.builder().name("Nájem").build()));
        expenseRepository.save(Expense.builder()
                .amount(20000)
                .expenseType(rentType)
                .isMandatory(true)
                .client(testClient)
                .build());

        mockMvc.perform(get("/api/v1/clients/{clientId}/budget", testClient.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncomes").value(50000))
                .andExpect(jsonPath("$.totalExpenses").value(20000))
                .andExpect(jsonPath("$.netCashflow").value(30000)) // 50000 - 20000
                .andExpect(jsonPath("$.incomes", hasSize(1)))
                .andExpect(jsonPath("$.expenses", hasSize(1)))
                .andExpect(jsonPath("$.incomes[0].type").value("Zaměstnání"))
                .andExpect(jsonPath("$.expenses[0].isMandatory").value(true));
    }

    @Test
    @WithMockUser(username = "ROGUE_99", roles = "ADVISOR")
    void getBudget_Fails_WhenAdvisorDoesNotOwnClient() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}/budget", testClient.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access Denied"))
                .andExpect(jsonPath("$.detail").value("Assigned advisor mismatch for client budget access"));
    }
}
