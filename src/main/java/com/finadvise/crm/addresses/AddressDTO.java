package com.finadvise.crm.addresses;

public record AddressDTO(
        Long id,                 // Useful if the frontend needs to reference the existing immutable address
        String streetName,       // Flattened from Street entity
        String cityName,         // Flattened from City entity
        Integer psc,             // Flattened from City entity
        Integer houseNumber,     // From Address entity
        Integer orientationNumber // From Address entity
) {}
