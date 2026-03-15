package com.finadvise.crm.addresses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressDTO(
        Long id,

        @NotBlank(message = "Street name is required")
        @Size(max = 100)
        String street,

        @NotBlank(message = "House number is required")
        @Size(max = 10)
        @Pattern(
                regexp = "^[1-9]\\d{0,3}(/[1-9]\\d{0,3}[a-z]?)?$",
                message = "Invalid Czech house number format (e.g., 1234 or 1234/15a)."
        )
        String houseNumber,

        @NotBlank(message = "City name is required")
        @Size(max = 100)
        String city,

        @NotBlank(message = "Postal code (PSČ) is required")
        @Pattern(regexp = "^\\d{3}\\s\\d{2}$", message = "Format: 123 45")
        String postalCode
) {}
