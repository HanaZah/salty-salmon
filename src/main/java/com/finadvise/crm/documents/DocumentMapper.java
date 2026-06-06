package com.finadvise.crm.documents;

import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentDTO toDto(Document document) {
        if (document == null) return null;

        Long productId = document.getProduct() != null ?
                document.getProduct().getId() : null;

        String productName = document.getProduct() != null ?
                document.getProduct().getName() : null;

        // Backend does the work, frontend just receives the result
        DocumentFormat format = DocumentFormat.fromFileName(document.getFileName());

        return new DocumentDTO(
                document.getId(),
                document.getFileName(),
                format,
                document.getUploadedAt(),
                document.getDocumentType().getId(),
                document.getDocumentType().getName(),
                productId,
                productName
        );
    }
}
