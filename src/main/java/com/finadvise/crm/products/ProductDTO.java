package com.finadvise.crm.products;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductDTO(
        Long id,

        @NotBlank(message = "Name is required")
        @Size(max = 150)
        String name,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "99999999.99")
        BigDecimal amount,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "Product type is required")
        Long productTypeId,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        String productTypeName, // Read-only

        @NotNull(message = "Provider is required")
        Long providerId,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        String providerName, // Read-only

        String managedByEmployeeId // Writable but optional: null means the product is externally managed or direct
) {}