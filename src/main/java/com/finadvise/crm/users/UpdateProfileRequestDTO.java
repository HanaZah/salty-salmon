package com.finadvise.crm.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDTO(
        @NotNull(message = "Version is required for concurrency control")
        Integer version,

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must be at most 50 characters long")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\s\\-']+$",
                message = "First name contains invalid characters." +
                        "Please use only standard letters, possibly hyphen or apostrophe."
        )
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must be at most 50 characters long")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\s\\-']+$",
                message = "Last name contains invalid characters." +
                        "Please use only standard letters, possibly hyphen or apostrophe."
        )
        String lastName,

        @NotBlank(message = "Phone is required")
        @Size(max = 20, message = "Phone must be at most 20 characters long")
        @Pattern(
                regexp = "^\\+?[\\d\\s\\-]+$",
                message = "Phone number can only contain digits, spaces, hyphens, and an optional leading plus sign"
        )
        String phone
) {}
