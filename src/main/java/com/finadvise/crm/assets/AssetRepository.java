package com.finadvise.crm.assets;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Page<Asset> findAllByClientId(Long clientId, Pageable pageable);
    Boolean existsByIdAndClientClientUid(Long id, String clientUid);
    @Query("SELECT COALESCE(SUM(a.value), 0) FROM Asset a WHERE a.client.id = :clientId")
    Long sumValueByClientId(@Param("clientId") Long clientId);
}
