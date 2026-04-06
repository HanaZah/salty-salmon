package com.finadvise.crm.budget;

import com.finadvise.crm.addresses.*;
import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.users.Advisor;
import com.finadvise.crm.users.AdvisorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import java.time.LocalDate;

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
    private ClientRepository clientRepository;

    @Autowired
    private AdvisorRepository advisorRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private StreetRepository streetRepository;

    @Autowired
    private IncomeTypeRepository incomeTypeRepository;

    @Autowired
    private ExpenseTypeRepository expenseTypeRepository;

    private Client initializeTestClient(Long id, String uid, String personalId, String lastName, Advisor advisor) {
        City testCity = cityRepository.save(City.builder().name("Prague").psc("123 45").build());
        Street testStreet = streetRepository.save(Street.builder().city(testCity).name("Vodičkova").build());
        Address testAddress = addressRepository.save(Address.builder().street(testStreet).houseNumber("123/45").build());

        return Client.builder()
                .id(id)
                .clientUid(uid)
                .personalId(personalId)
                .birthDate(LocalDate.now().minusYears(20))
                .firstName("Budget")
                .lastName(lastName)
                .phone("123456789")
                .email("budget@" + lastName + ".mail")
                .idCardNumber("000000000")
                .idCardIssueDate(LocalDate.now().minusYears(2))
                .idCardExpiryDate(LocalDate.now().plusYears(13))
                .idCardIssuer("Some Issuer")
                .lastUpdate(LocalDate.now())
                .version(0)
                .isActive(true)
                .advisor(advisor)
                .permanentAddress(testAddress)
                .contactAddress(testAddress)
                .build();
    }

    private Advisor initializeTestAdvisor(Long id, String employeeId, String ico, String lastName) {
        return Advisor.builder()
                .ico(ico)
                .manager(null)
                .id(id)
                .employeeId(employeeId)
                .passwordHash("hashed-pass")
                .firstName("Budget")
                .lastName(lastName)
                .phone("123456789")
                .email("budget@" + lastName + ".mail")
                .version(0)
                .isActive(true)
                .build();
    }

    @Test
    void getBudget_CalculatesTotalsCorrectly_WhenAdvisorEmployeeIdMatches() {
        Advisor testAdvisor = initializeTestAdvisor(234L, "BGADV_01", "12345678", "Advisor");
        advisorRepository.save(testAdvisor);

        Client testClient = initializeTestClient(1L, "BGCLI-01", "0000000000", "Happy", testAdvisor);
        clientRepository.save(testClient);

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

        ExpenseType testExpenseType = expenseTypeRepository.save(ExpenseType.builder().name("TestExoense").build());
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
        Advisor testAdvisor = initializeTestAdvisor(345L, "BGADV_02", "23456789", "Correct");
        advisorRepository.save(testAdvisor);

        Advisor anotherTestAdvisor = initializeTestAdvisor(456L, "BGADV_03", "34567890", "Wrong");
        advisorRepository.save(anotherTestAdvisor);

        Client testClient = initializeTestClient(22L, "BGCLI-02", "2222222222", "Sad", testAdvisor);
        clientRepository.save(testClient);

        assertThatThrownBy(() -> budgetService.getBudget(testClient.getId(), anotherTestAdvisor.getEmployeeId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Assigned advisor mismatch for client budget access");
    }
}
