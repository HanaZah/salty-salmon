package com.finadvise.crm.assets;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetDTO(

        Long id,

        @NotBlank(message = "Asset name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotNull(message = "Asset value is required")
        @Min(value = 1, message = "Asset value must be at least 1")
        @Max(value = 999999999, message = "Asset value exceeds maximum allowed limit")
        Integer value,

        @Size(max = 256, message = "Note must not exceed 256 characters")
        String note,

        @NotNull(message = "Asset type ID is required")
        Long assetTypeId,

        // Read-only field for UI to display a localized type name
        String assetTypeName
) {}
