package com.finadvise.crm.budget;

import java.math.BigDecimal;

public record BudgetItemDTO(
        Long id,
        String type,
        Integer amount,
        boolean isMandatory,
        Integer version
) {}
