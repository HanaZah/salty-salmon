package com.finadvise.crm.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpenseTypeRepository extends JpaRepository<ExpenseType, Long> {
    Optional<ExpenseType> findByName(String name);
}
