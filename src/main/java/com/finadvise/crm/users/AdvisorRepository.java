package com.finadvise.crm.users;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvisorRepository extends JpaRepository<Advisor, Long> {
    Boolean existsByIco(String ico);

    Boolean existsByEmail(String email);
}
