package com.finadvise.crm.users;

public record AdminContactDTO(
        String firstName,
        String lastName,
        String phone,
        String email
) {}
