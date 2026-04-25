package com.finadvise.crm.clients;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    @Query("SELECT c.id FROM Client c WHERE c.clientUid = :clientUid")
    Optional<Long> findIdByClientUid(@Param("clientUid") String clientUid);
    Boolean existsByClientUidAndAdvisorEmployeeId(String clientUid, String advisorEmployeeId);
}
