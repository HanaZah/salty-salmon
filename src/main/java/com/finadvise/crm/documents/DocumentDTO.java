package com.finadvise.crm.documents;

import java.time.LocalDate;

public record DocumentDTO(
        Long id,
        String fileName,
        DocumentFormat format, // Explicitly passed to the UI for icon rendering
        LocalDate uploadedAt,
        Long documentTypeId,
        String documentTypeName,
        Long productId,
        String productName
) {}
