package com.finadvise.crm.documents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FileMetadataDTO(
        @NotNull(message = "File name is required")
        @NotBlank(message = "File name cannot be blank")
        String fileName,

        @NotNull(message = "Document type ID is required")
        Long documentTypeId,
        Long productId
) {}
