package com.finadvise.crm.users;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(
        @NotNull(message = "Original password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String oldPassword,

        @NotNull(message = "New password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String newPassword
) {}
