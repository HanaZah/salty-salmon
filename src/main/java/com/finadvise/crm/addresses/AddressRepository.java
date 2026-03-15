package com.finadvise.crm.addresses;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByHouseNumberAndStreetId(
            String houseNumber,
            Long streetId
    );
}
