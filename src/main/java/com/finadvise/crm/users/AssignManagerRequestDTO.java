package com.finadvise.crm.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignManagerRequestDTO(
        @NotBlank(message = "Employee ID is required")
        @Size(max = 20, message = "Employee ID has wrong size or pattern")
        String managerEmployeeId
) {}
