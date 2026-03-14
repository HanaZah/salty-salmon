package com.finadvise.crm.addresses;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {

    // Spring's Dependency Injection handles wiring these up automatically
    private final CityRepository cityRepository;
    private final StreetRepository streetRepository;
    private final AddressRepository addressRepository;

    /**
     * Resolves an AddressDTO into a managed Address entity.
     * It looks up existing records to prevent unique constraint violations,
     * creating new ones only if they don't already exist.
     */
    @Transactional
    public Address findOrCreateAddress(AddressDTO dto) {
        if (dto == null) {
            return null;
        }

        City city = cityRepository.findByNameAndPsc(dto.cityName(), dto.psc())
                .orElseGet(() -> {
                    City newCity = City.builder()
                            .name(dto.cityName())
                            .psc(dto.psc())
                            .build();
                    return cityRepository.save(newCity);
                });

        Street street = streetRepository.findByNameAndCityId(dto.streetName(), city.getId())
                .orElseGet(() -> {
                    Street newStreet = Street.builder()
                            .name(dto.streetName())
                            .city(city)
                            .build();
                    return streetRepository.save(newStreet);
                });

        // 3. Find or Create Address
        return addressRepository.findByHouseNumberAndOrientationNumberAndStreetId(
                        dto.houseNumber(), dto.orientationNumber(), street.getId())
                .orElseGet(() -> {
                    Address newAddress = Address.builder()
                            .houseNumber(dto.houseNumber())
                            .orientationNumber(dto.orientationNumber())
                            .street(street)
                            .build();
                    return addressRepository.save(newAddress);
                });
    }
}
