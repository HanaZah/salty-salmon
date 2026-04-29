package com.finadvise.crm.users;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(
        @NotNull(message = "Original password is required")
        @Size(max = 72, message = "Password must be at most 72 characters long")
        String oldPassword,

        @NotNull(message = "New password is required")
        @Size(max = 72, message = "Password must be at most 72 characters long")
        String newPassword
) {}
