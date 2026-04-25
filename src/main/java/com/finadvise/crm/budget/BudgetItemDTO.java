package com.finadvise.crm.budget;

import jakarta.validation.constraints.NotNull;

public record BudgetItemDTO(
        Long id,
        @NotNull String type,
        @NotNull Integer amount,
        Boolean isMandatory,
        Integer version
) {}
