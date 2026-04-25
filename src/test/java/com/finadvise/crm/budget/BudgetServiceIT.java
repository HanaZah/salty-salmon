package com.finadvise.crm.budget;

import com.finadvise.crm.addresses.*;
import com.finadvise.crm.clients.Client;
import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.users.Advisor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BudgetServiceIT {
    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private IncomeTypeRepository incomeTypeRepository;

    @Autowired
    private ExpenseTypeRepository expenseTypeRepository;

    @Autowired
    private TestFixtureFactory testFixtureFactory;

    @Test
    void getBudget_CalculatesTotalsCorrectly_WhenAdvisorEmployeeIdMatches() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                234L, "BGADV_01", "12345678", "Advisor");

        Client testClient = testFixtureFactory.getOrCreateTestClient(
                1L, "BGCLI-01", "0000000000", "000000000", "Happy", testAdvisor);

        IncomeType testIncomeType = incomeTypeRepository.save(IncomeType.builder().name("TestIncome").build());
        incomeRepository.save(Income.builder()
                .incomeType(testIncomeType)
                .amount(4500)
                .client(testClient)
                .build());

        incomeRepository.save(Income.builder()
                .incomeType(testIncomeType)
                .amount(500)
                .client(testClient)
                .build());

        ExpenseType testExpenseType = expenseTypeRepository.save(ExpenseType.builder().name("TestExpense").build());
        expenseRepository.save(Expense.builder()
                .expenseType(testExpenseType)
                .amount(2500)
                .client(testClient)
                .build());

        expenseRepository.save(Expense.builder()
                .expenseType(testExpenseType)
                .amount(500)
                .client(testClient)
                .build());

        BudgetFullDTO result = budgetService.getBudget(testClient.getId(), testAdvisor.getEmployeeId());

        assertThat(result.totalIncomes()).isEqualByComparingTo("5000.00");
        assertThat(result.netCashflow()).isEqualByComparingTo("2000.00");
    }

    @Test
    void getBudget_ThrowsAccessDeniedException_WhenAdvisorEmployeeIdDoesNotMatch() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                345L, "BGADV_02", "23456789", "Correct");

        Advisor anotherTestAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                456L, "BGADV_03", "34567890", "Wrong");

        Client testClient = testFixtureFactory.getOrCreateTestClient(
                22L, "BGCLI-02", "2222222222", "222222222", "Sad", testAdvisor);

        assertThatThrownBy(() -> budgetService.getBudget(testClient.getId(), anotherTestAdvisor.getEmployeeId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Assigned advisor mismatch for client budget access");
    }
}
