package com.finadvise.crm.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdvisorRepository extends JpaRepository<Advisor, Long> {
    Boolean existsByIco(String ico);

    @Query("SELECT a.id FROM Advisor a WHERE a.employeeId = :employeeId")
    Optional<Long> findIdByEmployeeId(@Param("employeeId") String employeeId);

    Optional<Advisor> findByEmployeeId(String employeeId);

    Boolean existsByEmployeeIdAndManagerEmployeeId(String employeeId, String managerEmployeeId);
}
