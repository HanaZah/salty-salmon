package com.finadvise.crm.users;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdvisorRepository extends JpaRepository<Advisor, Long> {
    Optional<Advisor> findByIco(String ico);

    Boolean existsByIco(String ico);

    Boolean existsByEmail(String email);
}
