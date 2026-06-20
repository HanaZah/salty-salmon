package com.finadvise.crm.documents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record FileMetadataDTO(
        @NotNull(message = "File name is required")
        @NotBlank(message = "File name cannot be blank")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\p{N}\\s_.\\-]+$",
                message = "File name contains invalid characters. " +
                        "Only letters, numbers, spaces, dots, hyphens, and underscores are allowed."
        )
        String fileName,

        @NotNull(message = "Document type ID is required")
        Long documentTypeId,
        Long productId
) {}
