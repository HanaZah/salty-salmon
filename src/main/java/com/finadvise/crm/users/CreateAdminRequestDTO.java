package com.finadvise.crm.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminRequestDTO(
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must be at most 50 characters long")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must be at most 50 characters long")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password must be at most 72 characters long")
        String rawPassword
) {}
