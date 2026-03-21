package com.finadvise.crm.users;

import jakarta.validation.constraints.NotNull;

public record UpdateProfileRequestDTO(
        @NotNull(message = "First name is required")
        String firstName,

        @NotNull(message = "Last name is required")
        String lastName,

        String phone // Nullable if the user is an Admin
) {}
