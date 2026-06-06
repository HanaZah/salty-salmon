package com.finadvise.crm.documents;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("""
        SELECT COUNT(d) > 0
        FROM Document d
        JOIN d.client c
        JOIN c.advisor a
        LEFT JOIN d.product p
        LEFT JOIN p.managedBy m
        WHERE d.id = :documentId
          AND d.isActive = true
          AND c.clientUid = :clientUid
          AND (
              a.employeeId = :employeeId
              OR m.employeeId = :employeeId
          )
    """)
    boolean canAccessDocument(
            @Param("documentId") Long documentId,
            @Param("clientUid") String clientUid,
            @Param("employeeId") String employeeId
    );

    @Query("""
        SELECT d FROM Document d
        JOIN d.client c
        JOIN c.advisor a
        LEFT JOIN d.product p
        LEFT JOIN p.managedBy m
        WHERE c.clientUid = :clientUid
          AND d.isActive = true
          AND (
              a.employeeId = :employeeId
              OR m.employeeId = :employeeId
          )
    """)
    List<Document> findSecurelyByClientUid(
            @Param("clientUid") String clientUid,
            @Param("employeeId") String employeeId
    );

    Optional<Document> findByIdAndIsActiveTrue(Long id);

    List<Document> findAllByStorageDeletedAtNullAndIsActiveFalse();

    List<Document> findAllByClientClientUidAndIsActiveTrue(String clientUid);
}
