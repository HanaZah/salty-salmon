package com.finadvise.crm.clients;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    @Query("SELECT c.id FROM Client c WHERE c.clientUid = :clientUid")
    Optional<Long> findIdByClientUid(@Param("clientUid") String clientUid);
    Optional<Client> findByClientUidAndIsActiveTrue(String clientUid);

    Boolean existsByClientUidAndAdvisorEmployeeId(String clientUid, String advisorEmployeeId);

    @Query("""
        SELECT c.clientUid AS clientUid,
               CONCAT(c.firstName, ' ', c.lastName) AS fullName,
               c.occupation AS occupation,
               (SELECT COUNT(p) FROM Product p WHERE p.client = c AND (p.endDate IS NULL OR p.endDate > CURRENT_DATE)) AS activeProductsCount,
               (SELECT COALESCE(SUM(a.value), 0) FROM Asset a WHERE a.client = c) AS totalAssetsValue,
               (SELECT COALESCE(SUM(i.amount), 0) FROM Income i WHERE i.client = c) AS totalIncome,
               (SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.client = c) AS totalExpense,
               c.lastUpdate AS lastUpdate
        FROM Client c
        WHERE c.advisor.employeeId = :employeeId
    """)
    Page<ClientDashboardSummary> findClientSummariesByEmployeeId(@Param("employeeId") String employeeId, Pageable pageable);

    @Query("""
        SELECT c.clientUid AS clientUid,
               CONCAT(c.firstName, ' ', c.lastName) AS fullName,
               c.occupation AS occupation,
               (SELECT COUNT(p) FROM Product p WHERE p.client = c AND (p.endDate IS NULL OR p.endDate > CURRENT_DATE)) AS activeProductsCount,
               (SELECT COALESCE(SUM(a.value), 0) FROM Asset a WHERE a.client = c) AS totalAssetsValue,
               (SELECT COALESCE(SUM(i.amount), 0) FROM Income i WHERE i.client = c) AS totalIncome,
               (SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.client = c) AS totalExpense,
               c.lastUpdate AS lastUpdate
        FROM Client c
    """)
    Page<ClientDashboardSummary> findAllClientSummaries(Pageable pageable);

    boolean existsByPersonalId(String personalId);

    @Query(value = "SELECT CLIENT_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Long getNextSequenceValue();

    Slice<Client> findByNextBirthdayLessThanAndIsActiveTrue(LocalDate date, Pageable pageable);
}
