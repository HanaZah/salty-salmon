package com.finadvise.crm.clients;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ClientUpdateIdCardRequestDTO(
        @NotBlank(message = "ID card number is required")
        @Pattern(regexp = "^\\d{9}$", message = "ID card number must be exactly 9 digits")
        String idCardNumber,

        @NotNull(message = "Issue date is required")
        @PastOrPresent(message = "Issue date cannot be in the future")
        LocalDate idCardIssueDate,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        LocalDate idCardExpiryDate,

        @NotBlank(message = "Issuer is required")
        @Size(max = 100, message = "Issuer name cannot exceed 100 characters")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\p{N}\\s\\-.,'/]+$",
                message = "ID card issuer contains invalid characters." +
                        "Please use only standard letters, numbers, and basic punctuation."
        )
        String idCardIssuer,

        @NotNull(message = "Version is required for concurrency control")
        Integer version
) {}
