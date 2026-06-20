package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressInputDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ClientUpdateDetailsRequestDTO(
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\s\\-']+$",
                message = "First name contains invalid characters." +
                        "Please use only standard letters, possibly hyphen or apostrophe."
        )
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\s\\-']+$",
                message = "Last name contains invalid characters." +
                        "Please use only standard letters, possibly hyphen or apostrophe."
        )
        String lastName,

        @NotBlank(message = "Occupation is required")
        @Size(max = 100, message = "Occupation cannot exceed 100 characters")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\p{N}\\s\\-.,'/]+$",
                message = "Occupation contains invalid characters." +
                        "Please use only standard letters, numbers, and basic punctuation."
        )
        String occupation,

        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        @Pattern(
                regexp = "^\\+?[\\d\\s\\-]+$",
                message = "Phone number can only contain digits, spaces, hyphens, and an optional leading plus sign"
        )
        String phone,

        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        @NotNull(message = "Permanent address must be provided")
        @Valid
        AddressInputDTO permanentAddress,

        @NotNull(message = "Contact address must be provided")
        @Valid
        AddressInputDTO contactAddress
) {}
