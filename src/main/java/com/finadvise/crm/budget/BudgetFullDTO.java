package com.finadvise.crm.budget;

import java.math.BigDecimal;
import java.util.List;

public record BudgetFullDTO(
        BigDecimal totalIncomes,
        BigDecimal totalExpenses,
        BigDecimal netCashflow,
        List<BudgetItemDTO> incomes,
        List<BudgetItemDTO> expenses
) {}
