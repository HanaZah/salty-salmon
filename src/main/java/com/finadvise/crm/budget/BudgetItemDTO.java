package com.finadvise.crm.budget;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BudgetItemDTO(
        Long id,

        @NotBlank(message = "Budget item type is required")
        String type,

        @NotNull(message = "Budget item amount is required")
        @Min(value = 0, message = "Amount cannot be negative")
        Integer amount,

        Boolean isMandatory,
        Integer version
) {}
