package com.finadvise.crm.budget;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BudgetItemDTO(
        Long id,

        @NotNull(message = "Budget item type ID is required")
        Long typeId,

        // Read-only field for UI to display a localized type name
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        String typeName,

        // Database permits amount >= 1, but to allow full state API calls from UI ("tombstone" pattern),
        // we need the 0 amount to signify a deleted/empty budget item - it can never reach the database though
        @Min(value = 0, message = "Amount cannot be negative")
        @NotNull(message = "Budget item amount is required")
        Integer amount,

        Boolean isMandatory,
        Integer version
) {}
