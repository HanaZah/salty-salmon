package com.finadvise.crm.assets;

import org.springframework.data.domain.Page;

public record ClientAssetsDTO(
        String clientUid,
        Page<AssetDTO> assets,
        Long totalValue
) {}
