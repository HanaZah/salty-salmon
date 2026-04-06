package com.finadvise.crm.budget;

import com.finadvise.crm.common.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetMapper budgetMapper;
    private final OwnershipValidator ownershipValidator;

    @Transactional(readOnly = true)
    public BudgetFullDTO getBudget(Long clientId, String requesterEmployeeId) throws AccessDeniedException {
        if(!ownershipValidator.canAccessClient(clientId, requesterEmployeeId)) {
            throw new AccessDeniedException("Assigned advisor mismatch for client budget access");
        }

        List<BudgetItemDTO> incomes = incomeRepository.findAllByClientId(clientId).stream()
                .map(budgetMapper::toDto)
                .toList();
        List<BudgetItemDTO> expenses = expenseRepository.findAllByClientId(clientId).stream()
                .map(budgetMapper::toDto)
                .toList();
        BudgetSummaryProjection summary = budgetRepository.getSummaryByClientId(clientId);

        return new BudgetFullDTO(
                summary != null ? summary.getTotalIncomes() : BigDecimal.ZERO,
                summary != null ? summary.getTotalExpenses() : BigDecimal.ZERO,
                summary != null ? summary.getNetCashflow() : BigDecimal.ZERO,
                incomes,
                expenses
        );
    }
}
