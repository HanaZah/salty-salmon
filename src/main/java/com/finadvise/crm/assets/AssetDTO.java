package com.finadvise.crm.assets;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record AssetDTO(

        Long id,

        @NotBlank(message = "Asset name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\p{N}\\s\\-.,']+$",
                message = "Asset name contains invalid characters. " +
                        "Please use only standard letters, numbers, and basic punctuation."
        )
        String name,

        @NotNull(message = "Asset value is required")
        @Min(value = 1, message = "Asset value must be at least 1")
        @Max(value = 999999999, message = "Asset value exceeds maximum allowed limit")
        Integer value,

        @Size(max = 256, message = "Note must not exceed 256 characters")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\p{N}\\s\\-.,'/]+$",
                message = "Note contains invalid characters. " +
                        "Please use only standard letters, numbers, and basic punctuation."
        )
        String note,

        @NotNull(message = "Asset type ID is required")
        Long assetTypeId,

        // Read-only field for UI to display a localized type name
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        String assetTypeName
) {}
