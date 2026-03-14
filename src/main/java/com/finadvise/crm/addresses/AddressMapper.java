package com.finadvise.crm.addresses;

import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressDTO toDto(Address address) {
        if (address == null) {
            return null;
        }

        String streetName = null;
        String cityName = null;
        Integer psc = null;

        Street street = address.getStreet();
        if (street != null) {
            streetName = street.getName();

            City city = street.getCity();
            if (city != null) {
                cityName = city.getName();
                psc = city.getPsc();
            }
        }

        return new AddressDTO(
                address.getId(),
                streetName,
                cityName,
                psc,
                address.getHouseNumber(),
                address.getOrientationNumber()
        );
    }
}