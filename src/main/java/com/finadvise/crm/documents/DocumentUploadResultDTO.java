package com.finadvise.crm.documents;

import java.time.LocalDate;

public record DocumentUploadResultDTO(
        String filePath, // The safe S3 key
        LocalDate uploadedAt,
        FileMetadataDTO metadata
) {}
