package com.finadvise.crm.budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateBudgetRequestDTO(
        @NotNull(message = "Incomes list must be provided (can be empty)")
        @Valid
        List<BudgetItemDTO> incomes,

        @NotNull(message = "Expenses list must be provided (can be empty)")
        @Valid
        List<BudgetItemDTO> expenses
) {}
