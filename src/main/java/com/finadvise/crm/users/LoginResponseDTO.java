package com.finadvise.crm.users;

public record LoginResponseDTO(
        String token,
        String name,
        String employeeId,
        long expiresInSeconds    // helps the frontend know when to log the user out
) {}
