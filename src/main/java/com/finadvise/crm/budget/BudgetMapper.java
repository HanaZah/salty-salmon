package com.finadvise.crm.budget;

import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {

    public BudgetItemDTO toDto(Income income) {
        if (income == null) return null;
        return new BudgetItemDTO(
                income.getId(),
                income.getIncomeType().getId(),
                income.getIncomeType().getName(),
                income.getAmount(),
                false,
                income.getVersion()
        );
    }

    public BudgetItemDTO toDto(Expense expense) {
        if (expense == null) return null;
        return new BudgetItemDTO(
                expense.getId(),
                expense.getExpenseType().getId(),
                expense.getExpenseType().getName(),
                expense.getAmount(),
                expense.isMandatory(),
                expense.getVersion()
        );
    }
}
