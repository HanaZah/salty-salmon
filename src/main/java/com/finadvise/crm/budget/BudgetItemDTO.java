package com.finadvise.crm.budget;

public record BudgetItemDTO(
        Long id,
        String type,
        Integer amount,
        boolean isMandatory,
        Integer version
) {}
