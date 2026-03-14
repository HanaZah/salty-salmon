package com.finadvise.crm.addresses;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StreetRepository extends JpaRepository<Street, Long> {

    Optional<Street> findByNameAndCityId(String name, Long cityId);
}
