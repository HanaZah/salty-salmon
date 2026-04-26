package com.finadvise.crm.assets;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findAllByClientId(Long clientId);
    Boolean existsByIdAndClientClientUid(Long id, String clientUid);
}
