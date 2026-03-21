package com.finadvise.crm.users;

import jakarta.validation.constraints.NotBlank;

public record CreateAdminRequestDTO(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Password is required")
        String rawPassword
) {}
