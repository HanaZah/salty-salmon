package com.finadvise.crm.products;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

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

    boolean existsByClientClientUidAndManagedByEmployeeId(String clientUid, String employeeId);

    List<Product> findAllByClientClientUidAndManagedByEmployeeId(String clientUid, String employeeId);
    List<Product> findAllByClientClientUid(String clientUid);
}
