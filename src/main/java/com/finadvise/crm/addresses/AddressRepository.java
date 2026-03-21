package com.finadvise.crm.addresses;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByHouseNumberAndStreetId(
            String houseNumber,
            Long streetId
    );
}
