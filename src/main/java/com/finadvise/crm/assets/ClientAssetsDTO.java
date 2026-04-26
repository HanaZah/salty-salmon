package com.finadvise.crm.assets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ClientAssetsDTO(

        @NotBlank
        String clientUid,

        @NotNull
        List<AssetDTO> assets,

        // Calculated by the service layer.
        // Uses Long to prevent Integer Overflow from multiple high-value assets.
        @NotNull
        Long totalValue
) {}
