package com.finadvise.crm.assets;

import java.util.List;

public record ClientAssetsDTO(
        String clientUid,

        List<AssetDTO> assets,

        // Calculated by the service layer.
        // Uses Long to prevent Integer Overflow from multiple high-value assets.
        Long totalValue
) {}
