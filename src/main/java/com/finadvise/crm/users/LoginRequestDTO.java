package com.finadvise.crm.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(

        @NotBlank(message = "Employee ID is required")
        @Size(max = 20, message = "Employee ID has wrong size or pattern")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "Employee ID has wrong size or pattern"
        )
        String employeeId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {}
