package com.finadvise.crm.users;

public record AdminDTO(
        Long id,
        String employeeId,
        String firstName,
        String lastName
) {}
