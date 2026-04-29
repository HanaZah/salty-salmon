package com.finadvise.crm.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(

        @NotBlank(message = "Employee ID is required")
        @Size(max = 20, message = "Employee ID has wrong size or pattern")
        String employeeId,

        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password must be at most 72 characters long")
        String password
) {}
