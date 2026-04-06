package com.finadvise.crm.users;

import java.util.List;

public record PasswordRecoveryResponseDTO(
        String message,
        List<String> adminEmails
) {}
