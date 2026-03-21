package com.finadvise.crm.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAdvisorRequestDTO(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "ICO is required")
        @Pattern(regexp = "^\\d{8}$", message = "ICO must be exactly 8 digits")
        String ico,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "Password is required")
        String rawPassword
) {}
