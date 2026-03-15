package com.finadvise.crm.addresses;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByNameAndPsc(String name, String psc);
}
