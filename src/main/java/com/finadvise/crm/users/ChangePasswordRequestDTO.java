package com.finadvise.crm.users;

import jakarta.validation.constraints.NotNull;

public record ChangePasswordRequestDTO(
        @NotNull(message = "Original password is required")
        String oldPassword,

        @NotNull(message = "New password is required")
        String newPassword
) {}
