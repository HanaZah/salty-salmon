package com.finadvise.crm.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssignManagerRequestDTO(
        @NotBlank(message = "Employee ID is required")
        @Size(max = 20, message = "Employee ID has wrong size or pattern")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "Employee ID has wrong size or pattern"
        )
        String managerEmployeeId
) {}
