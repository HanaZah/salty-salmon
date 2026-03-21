package com.finadvise.crm.users;

import jakarta.validation.constraints.NotNull;

public record LoginRequestDTO(

        @NotNull(message = "Employee ID is required")
        String employeeId,

        @NotNull(message = "Password is required")
        String password
) {}
