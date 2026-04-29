package com.finadvise.crm.budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record BudgetFullDTO(
        @NotBlank(message = "Client UID is required")
        @Size(max = 8, message = "Invalid UID format or length.")
        String clientUid,

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
