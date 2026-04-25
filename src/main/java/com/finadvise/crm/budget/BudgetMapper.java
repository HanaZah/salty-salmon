package com.finadvise.crm.budget;

import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {

    public BudgetItemDTO toDto(Income income) {
        if (income == null) {
            return null;
        }

        Long typeId = null;
        String typeName = null;

        if (income.getIncomeType() != null) {
            typeId = income.getIncomeType().getId();
            typeName = income.getIncomeType().getName();
        }

        return new BudgetItemDTO(
                income.getId(),
                typeId,
                typeName,
                income.getAmount(),
                null, // Incomes don't have a mandatory flag
                income.getVersion()
        );
    }

    public BudgetItemDTO toDto(Expense expense) {
        if (expense == null) {
            return null;
        }

        Long typeId = null;
        String typeName = null;

        if (expense.getExpenseType() != null) {
            typeId = expense.getExpenseType().getId();
            typeName = expense.getExpenseType().getName();
        }

        return new BudgetItemDTO(
                expense.getId(),
                typeId,
                typeName,
                expense.getAmount(),
                expense.isMandatory(),
                expense.getVersion()
        );
    }
}
