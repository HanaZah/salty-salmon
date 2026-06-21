package com.finadvise.crm.products;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Slice<Product> findByNextAnniversaryLessThan(LocalDate date, Pageable pageable);

    @Query("""
        SELECT COUNT(p) > 0
        FROM Product p
        JOIN p.client c
        JOIN c.advisor a
        LEFT JOIN p.managedBy m
        WHERE p.id = :productId
          AND c.clientUid = :clientUid
          AND (
              m.employeeId = :employeeId
              OR (m IS NULL AND a.employeeId = :employeeId)
          )
    """)
    boolean canModifyProduct(
            @Param("productId") Long productId,
            @Param("clientUid") String clientUid,
            @Param("employeeId") String employeeId
    );

    @Query("""
        SELECT COUNT(p) > 0
        FROM Product p
        JOIN p.client c
        JOIN c.advisor a
        LEFT JOIN p.managedBy m
        WHERE p.id = :productId
          AND c.clientUid = :clientUid
          AND (
              m.employeeId = :employeeId
              OR a.employeeId = :employeeId
          )
    """)
    boolean canAccessProduct(
            @Param("productId") Long productId,
            @Param("clientUid") String clientUid,
            @Param("employeeId") String employeeId
    );

    boolean existsByClientClientUidAndManagedByEmployeeId(String clientUid, String employeeId);
    Page<Product> findAllByClientClientUid(String clientUid, Pageable pageable);
    Page<Product> findAllByClientClientUidAndManagedByEmployeeId(String clientUid, String employeeId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p " +
            "WHERE p.client.clientUid = :clientUid AND (p.endDate IS NULL OR p.endDate > CURRENT_DATE)")
    Integer countActiveByClientUid(@Param("clientUid") String clientUid);

    @Query("SELECT COUNT(p) FROM Product p " +
            "WHERE p.client.clientUid = :clientUid AND p.managedBy.employeeId = :employeeId AND (p.endDate IS NULL OR p.endDate > CURRENT_DATE)")
    Integer countActiveByClientUidAndAdvisor(@Param("clientUid") String clientUid, @Param("employeeId") String employeeId);

    boolean existsByIdAndClientClientUid(Long id, String clientUid);
}
