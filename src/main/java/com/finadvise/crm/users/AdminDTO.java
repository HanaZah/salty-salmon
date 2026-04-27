package com.finadvise.crm.users;

public record AdminDTO(
        String employeeId,
        String email,
        String firstName,
        String lastName
) {}
