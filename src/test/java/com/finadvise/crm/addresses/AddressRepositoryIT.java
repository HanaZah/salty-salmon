package com.finadvise.crm.addresses;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AddressRepositoryIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:latest");

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveAddress() {
        // 1. Setup the hierarchy
        City prague = City.builder()
                .psc("100 00")
                .name("Praha")
                .build();
        entityManager.persist(prague);

        Street thakurova = Street.builder()
                .name("Thákurova")
                .city(prague)
                .build();
        entityManager.persist(thakurova);

        Address address = Address.builder()
                .street(thakurova)
                .houseNumber("5")
                .build();

        // 2. Action
        Address savedAddress = addressRepository.save(address);
        entityManager.flush();
        entityManager.clear();

        // 3. Assertion
        Address retrieved = addressRepository.findById(savedAddress.getId()).orElseThrow();

        assertEquals("Thákurova", retrieved.getStreet().getName());
        assertEquals("Praha", retrieved.getStreet().getCity().getName());
        assertEquals("5", retrieved.getHouseNumber());
    }
}
