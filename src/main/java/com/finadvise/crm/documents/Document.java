package com.finadvise.crm.documents;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.products.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "DOCUMENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "document_gen")
    @SequenceGenerator(name = "document_gen", sequenceName = "DOCUMENT_SEQ", allocationSize = 1)
    @Column(name = "DOCUMENT_ID")
    private Long id;

    @Column(name = "FILE_NAME", nullable = false, length = 255)
    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name cannot exceed 255 characters")
    private String fileName;

    @Column(name = "FILE_PATH", nullable = false, length = 512)
    @NotBlank(message = "File path is required")
    @Size(max = 512, message = "File path cannot exceed 512 characters")
    private String filePath;

    @Column(name = "UPLOADED_AT", nullable = false)
    @NotNull(message = "Uploaded date is required")
    private LocalDate uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DOCUMENT_TYPE_ID", nullable = false)
    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    @NotNull(message = "Client is required")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    @Column(name = "IS_ACTIVE", nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "STORAGE_DELETED_AT")
    private LocalDateTime storageDeletedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDate.now();
        }
    }
}
