package com.finadvise.crm.budget;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BudgetItemDTO(
        Long id,

        @NotNull(message = "Budget item type ID is required")
        Long typeId,

        // Read-only field for UI to display a localized type name
        String typeName,

        @NotNull(message = "Budget item amount is required")
        @Min(value = 0, message = "Amount cannot be negative")
        Integer amount,

        Boolean isMandatory,
        Integer version
) {}
