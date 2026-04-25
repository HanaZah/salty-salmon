package com.finadvise.crm.budget;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.common.MissingVersionException;
import com.finadvise.crm.common.OwnershipValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private IncomeRepository incomeRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private IncomeTypeRepository incomeTypeRepository;
    @Mock private OwnershipValidator ownershipValidator;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void updateFullBudget_ThrowsOptimisticLocking_WhenVersionMismatch() {
        String clientUid = "CLI_01";
        Long clientId = 1L;
        String requesterId = "ADV_01";

        IncomeType incomeType = IncomeType.builder().id(1L).name("Salary").build();
        Income existingIncome = Income.builder().id(100L).version(2).incomeType(incomeType).build();

        when(ownershipValidator.canAccessClient(clientUid, requesterId)).thenReturn(true);
        when(incomeRepository.findAllByClientId(clientId)).thenReturn(List.of(existingIncome));
        when(clientRepository.findIdByClientUid(clientUid)).thenReturn(Optional.of(clientId));

        BudgetItemDTO staleDto = new BudgetItemDTO(100L, 1L,"Salary", 50000, null, 1);
        BudgetFullDTO request = new BudgetFullDTO(null, null, null, List.of(staleDto), List.of());

        assertThrows(ObjectOptimisticLockingFailureException.class, () ->
                budgetService.updateFullBudget(clientUid, request, requesterId)
        );

        verify(incomeRepository, never()).save(any());
        verify(incomeRepository, never()).delete(any());
    }

    @Test
    void updateFullBudget_AppliesUpdateDeleteAndCreate_Correctly() {
        Long clientId = 1L;
        String clientUid = "CLI_01";
        String requesterId = "ADV_01";

        IncomeType salaryType = IncomeType.builder().id(1L).name("Salary").build();
        Income incomeToUpdate = Income.builder().id(10L).amount(1000).version(1).incomeType(salaryType).build();
        Income incomeToDelete = Income.builder().id(11L).amount(500).version(1).incomeType(salaryType).build();

        when(ownershipValidator.canAccessClient(clientUid, requesterId)).thenReturn(true);
        when(incomeRepository.findAllByClientId(clientId)).thenReturn(List.of(incomeToUpdate, incomeToDelete));

        when(incomeTypeRepository.findById(1L)).thenReturn(Optional.of(salaryType));
        when(clientRepository.findIdByClientUid(clientUid)).thenReturn(Optional.of(clientId));
        when(clientRepository.getReferenceById(clientId)).thenReturn(new Client()); // Mock proxy

        // Setup DTO request from UI
        BudgetItemDTO updateDto = new BudgetItemDTO(
                10L, 1L, "Salary", 1500, null, 1); // Amount changed 1000 -> 1500
        BudgetItemDTO deleteDto = new BudgetItemDTO(
                11L, 1L, "Salary", 0, null, 1);   // Amount 0 -> should delete
        BudgetItemDTO createDto = new BudgetItemDTO(
                null, 1L, "Salary", 3000, null, null); // New item

        BudgetFullDTO request = new BudgetFullDTO(null, null, null,
                List.of(updateDto, deleteDto, createDto),
                List.of());

        budgetService.updateFullBudget(clientUid, request, requesterId);

        assertEquals(1500, incomeToUpdate.getAmount());
        verify(incomeRepository).delete(incomeToDelete);
        ArgumentCaptor<Income> incomeCaptor = ArgumentCaptor.forClass(Income.class);
        verify(incomeRepository).save(incomeCaptor.capture());

        Income savedIncome = incomeCaptor.getValue();
        assertEquals(3000, savedIncome.getAmount());
        assertEquals("Salary", savedIncome.getIncomeType().getName());
    }

    @Test
    void updateFullBudget_ThrowsMissingVersionException_WhenExistingItemLacksVersion() {
        Long clientId = 1L;
        String clientUid = "CLI_01";
        String requesterId = "ADV_01";

        Income existingIncome = Income.builder()
                .incomeType(IncomeType.builder().name("Salary").build())
                .id(100L)
                .amount(1000)
                .version(1)
                .build();

        when(ownershipValidator.canAccessClient(clientUid, requesterId)).thenReturn(true);
        when(clientRepository.findIdByClientUid(clientUid)).thenReturn(Optional.of(clientId));
        when(incomeRepository.findAllByClientId(clientId)).thenReturn(List.of(existingIncome));

        BudgetItemDTO malformedDto = new BudgetItemDTO(100L, 1L, "Salary", 50000, null, null);
        BudgetFullDTO request = new BudgetFullDTO(null, null, null, List.of(malformedDto), List.of());

        assertThrows(MissingVersionException.class, () ->
                budgetService.updateFullBudget(clientUid, request, requesterId)
        );
        verify(incomeRepository, never()).save(any());
        verify(incomeRepository, never()).delete(any());
    }
}
