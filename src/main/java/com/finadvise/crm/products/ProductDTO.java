package com.finadvise.crm.products;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductDTO(
        Long id,

        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must be at most 150 characters long")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\p{N}\\s\\-.,'/]+$",
                message = "Product name contains invalid characters." +
                        "Please use only standard letters, numbers, and basic punctuation."
        )
        String name,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", message = "Amount must be at least 0.00")
        @DecimalMax(value = "99999999.99", message = "Amount must not exceed 999,999,999.99")
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

        @Size(max = 20, message = "Employee ID has wrong size or pattern")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "Employee ID has wrong size or pattern"
        )
        String managedByEmployeeId // Writable but optional: null means the product is externally managed or direct
) {}