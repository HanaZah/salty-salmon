package com.finadvise.crm.products;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ProductSearchCriteriaDTO(
        List<Long> providerIds,

        List<Long> productTypeIds,

        @NotNull(message = "includeManagedProducts flag is mandatory")
        Boolean includeManagedProducts,

        LocalDate anniversaryDateFrom,

        LocalDate anniversaryDateTo
) {}
