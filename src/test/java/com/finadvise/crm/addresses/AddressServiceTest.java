package com.finadvise.crm.addresses;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock private CityRepository cityRepository;
    @Mock
    private StreetRepository streetRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private AddressMapper addressMapper;
    @Mock private ExternalAddressValidator addressValidator;

    @InjectMocks
    private AddressService addressService;

    private AddressDTO inputDto;
    private City mockCity;
    private Street mockStreet;
    private Address mockAddress;

    @BeforeEach
    void setUp() {
        inputDto = new AddressDTO(null, "Vodičkova", "123/45a", "Praha", "110 00");

        mockCity = City.builder().id(1L).name("Praha").psc("110 00").build();
        mockStreet = Street.builder().id(10L).name("Vodičkova").city(mockCity).build();
        mockAddress = Address.builder().id(100L).houseNumber("123/45a").street(mockStreet).build();
    }

    @Test
    @DisplayName("Should return existing address and not call save when all components exist")
    void findOrCreateAddress_ExistingRecords() {
        // Arrange
        when(cityRepository.findByNameAndPsc(anyString(), anyString())).thenReturn(Optional.of(mockCity));
        when(streetRepository.findByNameAndCityId(anyString(), anyLong())).thenReturn(Optional.of(mockStreet));
        when(addressRepository.findByHouseNumberAndStreetId(anyString(), anyLong())).thenReturn(Optional.of(mockAddress));
        when(addressMapper.toDto(any(Address.class))).thenReturn(new AddressDTO(100L, "Vodičkova", "123/45a", "Praha", "110 00"));

        // Act
        AddressDTO result = addressService.findOrCreateAddress(inputDto);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.id());
        verify(addressValidator).validate(inputDto);
        // Verify that SAVE was NEVER called because records existed
        verify(cityRepository, never()).save(any());
        verify(streetRepository, never()).save(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create new City, Street, and Address when none exist")
    void findOrCreateAddress_CreateAllNew() {
        // Arrange
        when(cityRepository.findByNameAndPsc(anyString(), anyString())).thenReturn(Optional.empty());
        when(streetRepository.findByNameAndCityId(anyString(), anyLong())).thenReturn(Optional.empty());
        when(addressRepository.findByHouseNumberAndStreetId(anyString(), anyLong())).thenReturn(Optional.empty());

        // Mocking the save calls to return our built objects (simulating DB ID generation)
        when(cityRepository.save(any(City.class))).thenReturn(mockCity);
        when(streetRepository.save(any(Street.class))).thenReturn(mockStreet);
        when(addressRepository.save(any(Address.class))).thenReturn(mockAddress);
        when(addressMapper.toDto(mockAddress)).thenReturn(new AddressDTO(100L, "Vodičkova", "123/45a", "Praha", "110 00"));

        // Act
        AddressDTO result = addressService.findOrCreateAddress(inputDto);

        // Assert
        assertNotNull(result);
        verify(cityRepository).save(any(City.class));
        verify(streetRepository).save(any(Street.class));
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("Should throw exception when validator fails")
    void findOrCreateAddress_ValidationFails() {
        // Arrange
        doThrow(new AddressValidationException("Invalid RÚIAN address"))
                .when(addressValidator).validate(any(AddressDTO.class));

        // Act & Assert
        assertThrows(AddressValidationException.class, () -> addressService.findOrCreateAddress(inputDto));
        verifyNoInteractions(cityRepository, streetRepository, addressRepository);
    }
}
