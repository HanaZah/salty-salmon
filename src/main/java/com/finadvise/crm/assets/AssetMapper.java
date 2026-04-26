package com.finadvise.crm.assets;

import org.springframework.stereotype.Component;

@Component
public class AssetMapper {

    public AssetDTO toDto(Asset asset) {
        if (asset == null) {
            return null;
        }

        Long typeId = null;
        String typeName = null;

        if (asset.getAssetType() != null) {
            typeId = asset.getAssetType().getId();
            typeName = asset.getAssetType().getName();
        }

        return new AssetDTO(
                asset.getId(),
                asset.getName(),
                asset.getValue(),
                asset.getNote(),
                typeId,
                typeName
        );
    }
}
