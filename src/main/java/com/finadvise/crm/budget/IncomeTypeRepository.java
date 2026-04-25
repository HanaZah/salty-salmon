package com.finadvise.crm.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncomeTypeRepository extends JpaRepository<IncomeType, Long> {
    Optional<IncomeType> findByName(String name);
}