package com.finadvise.crm.users;

public record AdvisorDTO(
        Long id,
        String employeeId,
        String ico,
        String firstName,
        String lastName,
        String phone,
        String email,
        Long managerId,
        AdvisorStatisticsDTO statistics // Nullable for lightweight API calls
) {}
