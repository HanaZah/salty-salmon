package com.finadvise.crm.documents;

import org.springframework.data.domain.Page;

public record ClientDocumentsDTO(
        String clientUid,
        Page<DocumentDTO> documents,
        Integer totalDocuments
) {}
