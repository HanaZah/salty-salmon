package com.finadvise.crm.clients;

import java.time.LocalDate;

public record ClientSearchMinimalDTO(
        String clientUid,
        String advisorEmployeeId,
        String personalId,
        String fullName,
        LocalDate birthDate,
        LocalDate nextBirthday,
        LocalDate idCardExpiryDate,
        LocalDate lastUpdate,
        String contactCityName,
        String contactPsc
) {}
