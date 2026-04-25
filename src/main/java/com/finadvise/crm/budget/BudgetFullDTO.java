package com.finadvise.crm.budget;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record BudgetFullDTO(
        // Optional for Writes (Server calculates these)
        BigDecimal totalIncomes,
        BigDecimal totalExpenses,
        BigDecimal netCashflow,

        // Mandatory for both Read/Write
        @NotNull List<BudgetItemDTO> incomes,
        @NotNull List<BudgetItemDTO> expenses
) {}
