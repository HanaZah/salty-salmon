package com.finadvise.crm.budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record BudgetFullDTO(
        // Optional for Writes (Server calculates these)
        BigDecimal totalIncomes,
        BigDecimal totalExpenses,
        BigDecimal netCashflow,

        // Mandatory for both Read/Write
        @NotNull(message = "Incomes list must be provided (can be empty)")
        @Valid
        List<BudgetItemDTO> incomes,

        @NotNull(message = "Expenses list must be provided (can be empty)")
        @Valid
        List<BudgetItemDTO> expenses
) {}
