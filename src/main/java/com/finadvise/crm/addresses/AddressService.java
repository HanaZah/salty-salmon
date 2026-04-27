package com.finadvise.crm.addresses;

import com.finadvise.crm.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final CityRepository cityRepository;
    private final StreetRepository streetRepository;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final ExternalAddressValidator addressValidator;

    /**
     * Resolves an AddressDTO into a managed Address entity.
     * It looks up existing records to prevent unique constraint violations,
     * creating new ones only if they don't already exist.
     */
    @Transactional
    public AddressDTO findOrCreateAddress(AddressDTO dto) {
        if (dto == null) {
            return null;
        }
        addressValidator.validate(dto);

        City city = cityRepository.findByNameAndPsc(dto.city(), dto.postalCode())
                .orElseGet(() -> {
                    City newCity = City.builder()
                            .name(dto.city())
                            .psc(dto.postalCode())
                            .build();
                    return cityRepository.save(newCity);
                });

        Street street = streetRepository.findByNameAndCityId(dto.street(), city.getId())
                .orElseGet(() -> {
                    Street newStreet = Street.builder()
                            .name(dto.street())
                            .city(city)
                            .build();
                    return streetRepository.save(newStreet);
                });

        Address address = addressRepository.findByHouseNumberAndStreetId(
                            dto.houseNumber(), street.getId())
                .orElseGet(() -> {
                    Address newAddress = Address.builder()
                            .houseNumber(dto.houseNumber())
                            .street(street)
                            .build();
                    return addressRepository.save(newAddress);
                });
        return addressMapper.toDto(address);
    }

    public AddressDTO getAddressById(Long id) {
        return addressRepository.findById(id)
                .map(addressMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}
