package com.finadvise.crm.documents;

import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.common.OwnershipValidator;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.products.ProductRepository;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final DocumentMapper documentMapper;
    private final OwnershipValidator ownershipValidator;
    private final S3Template s3Template;
    private final Tika tika;
    private final S3BucketProvisioner bucketProvisioner;
    private final Clock clock;

    @Value("${app.storage.bucket-name}")
    private String bucketName;

    @Value("${app.document.max-file-size-bytes:10485760}") // Default 10MB
    private long maxFileSizeBytes;

    @Value("${app.storage.download-url-expiration-seconds:60}")
    private long downloadUrlExpirationSeconds;


    @Transactional
    public List<DocumentDTO> saveAllDocuments(Long clientId, List<DocumentUploadResultDTO> uploadedFiles) {
        List<Document> documentsToSave = uploadedFiles.stream()
                .map(fileInfo -> Document.builder()
                        .fileName(fileInfo.metadata().fileName())
                        .filePath(fileInfo.filePath())
                        .uploadedAt(fileInfo.uploadedAt())
                        .documentType(documentTypeRepository.getReferenceById(fileInfo.metadata().documentTypeId()))
                        .product(fileInfo.metadata().productId() != null
                                ? productRepository.getReferenceById(fileInfo.metadata().productId())
                                : null)
                        .client(clientRepository.getReferenceById(clientId))
                        .build())
                .toList();

        return documentRepository.saveAll(documentsToSave).stream()
                .map(documentMapper::toDto)
                .toList();
    }

    public DocumentUploadResultDTO uploadDocument(String clientUid, String employeeId,
                                                  FileMetadataDTO metadata, MultipartFile file) {
        if (metadata.productId() != null
                && !ownershipValidator.canAccessProduct(metadata.productId(), clientUid, employeeId)) {
            throw new ResourceNotFoundException("Product not found or access denied");
        }
        if (metadata.productId() == null && !ownershipValidator.canAccessClient(clientUid, employeeId)) {
            throw new ResourceNotFoundException("Client not found or access denied");
        }
        if (!documentTypeRepository.existsById(metadata.documentTypeId())) {
            throw new ResourceNotFoundException("Document type not found");
        }

        validateFileSecurity(file);

        String safeStorageKey = String.format("clients/%s/%s-%s",
                clientUid, UUID.randomUUID(), metadata.fileName().replaceAll("[^a-zA-Z0-9.-]", "_"));
        try (InputStream inputStream = file.getInputStream()) {
            uploadToS3WithSelfHealing(safeStorageKey, inputStream);
        } catch (IOException e) {
            throw new UnreadableDocumentException("Failed to read the file stream", e);
        }

        return new DocumentUploadResultDTO(safeStorageKey, LocalDate.now(clock), metadata);
    }

    public void uploadToS3WithSelfHealing(String safeStorageKey, InputStream inputStream) {
        try {
            s3Template.upload(bucketName, safeStorageKey, inputStream);
        } catch (Exception e) {
            if (isNoSuchBucketError(e)) {
                log.warn("S3 Bucket missing during upload! Attempting self-healing recreation...");
                bucketProvisioner.ensureBucketExists();

                try {
                    s3Template.upload(bucketName, safeStorageKey, inputStream);
                    log.info("Self-healing successful. File uploaded.");
                } catch (Exception retryException) {
                    throw new DmsUnavailableException("Failed to store file after bucket recreation attempt.", retryException);
                }
            } else {
                throw new DmsUnavailableException("Failed to store file securely. Please try again.", e);
            }
        }
    }

    private boolean isNoSuchBucketError(Exception e) {
        // Dig into the cause to find the specific AWS SDK exception
        Throwable cause = e.getCause();
        return cause != null && cause.getClass().getSimpleName().equals("NoSuchBucketException");
    }

    public void validateFileSecurity(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnreadableDocumentException("Cannot upload an empty or missing file");
        }

        if (file.getSize() > maxFileSizeBytes) {
            log.warn("Security/Business Alert: File size {} exceeds limit of {}", file.getSize(), maxFileSizeBytes);
            throw new UnsupportedDocumentFormatException(
                    "File is too large. Maximum allowed size is " + (maxFileSizeBytes / 1024 / 1024) + "MB."
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new UnsupportedDocumentFormatException("File must have an extension.");
        }

        DocumentFormat format = DocumentFormat.fromFileName(originalFilename);
        if (format == DocumentFormat.UNKNOWN) {
            throw new UnsupportedDocumentFormatException(
                    "Unsupported file format. Please upload a PDF, image, Word, Excel, or CSV document."
            );
        }

        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        try {
            String detectedMimeTypeString = tika.detect(file.getInputStream());
            MimeType detectedMimeType = MimeTypes.getDefaultMimeTypes().forName(detectedMimeTypeString);

            boolean extensionMatchesContent = detectedMimeType.getExtensions().contains(fileExtension);

            // Special handling for CSVs: Tika often detects CSVs simply as "text/plain" which expects a ".txt" extension
            if (!extensionMatchesContent
                    && format == DocumentFormat.EXCEL
                    && detectedMimeTypeString.equals("text/plain")) {
                extensionMatchesContent = true;
            }

            if (!extensionMatchesContent) {
                log.warn("Security Alert: File extension spoofing detected. Name: {}, True Type: {}",
                        originalFilename, detectedMimeTypeString);
                throw new UnsupportedDocumentFormatException(
                        "File extension does not match the actual file content. " +
                        "This file is potentially malformed or malignant."
                );
            }

        } catch (IOException e) {
            throw new UnreadableDocumentException("Failed to read the file stream for security analysis.", e);
        } catch (MimeTypeException e) {
            throw new UnsupportedDocumentFormatException("Unknown file type detected.", e);
        }
    }

    @Transactional(readOnly = true)
    public DocumentDTO getDocumentById(String clientUid, Long documentId, String employeeId) {
        if (!ownershipValidator.canAccessDocument(documentId, clientUid, employeeId)) {
            throw new ResourceNotFoundException("Document not found or access denied");
        }

        return documentRepository.findById(documentId)
                .filter(Document::isActive)
                .map(documentMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found or access denied"));
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> getClientDocuments(String clientUid, String employeeId) {
        return documentRepository.findSecurelyByClientUid(clientUid, employeeId)
                .stream()
                .map(documentMapper::toDto)
                .toList();
    }

    @Transactional
    public DocumentDTO updateDocument(String clientUid, Long documentId, DocumentUpdateRequestDTO request,
                                      String employeeId) {
        if (!ownershipValidator.canAccessDocument(documentId, clientUid, employeeId)) {
            throw new ResourceNotFoundException("Document not found or access denied");
        }

        Document document = documentRepository.findById(documentId)
                .filter(Document::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found or access denied"));

        if (request.fileName() != null && !request.fileName().isBlank()) {
            document.setFileName(request.fileName());
        }

        if (request.productId() == null) {
            document.setProduct(null);
        } else {
            if (!ownershipValidator.canAccessProduct(request.productId(), document.getClient().getClientUid(), employeeId)) {
                throw new ResourceNotFoundException("Product not found or access denied");
            }
            document.setProduct(productRepository.getReferenceById(request.productId()));
        }

        return documentMapper.toDto(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public String generateDownloadUrl(String clientUid, Long documentId, String employeeId) {
        if (!ownershipValidator.canAccessDocument(documentId, clientUid, employeeId)) {
            throw new ResourceNotFoundException("Document not found or access denied");
        }

        Document document = documentRepository.findByIdAndIsActiveTrue(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found or access denied"));

        try {
            /* * ARCHITECTURAL NOTE: Optimistic Execution Pattern
             * We intentionally do NOT call s3Template.objectExists() here before generating the URL.
             * Generating a Pre-Signed URL is a purely local cryptographic operation (~1ms).
             * Checking object existence requires a synchronous HTTP HEAD request to AWS (~20-50ms blocking I/O).
             * To protect the thread pool under high concurrent load, we blindly assume the file exists.
             * If the file is missing from S3 (Data Divergence/Split-Brain scenario), AWS will safely return
             * an XML HTTP 404 (NoSuchKey) directly to the frontend when it attempts the download.
             */
            Duration timeToLive = Duration.ofSeconds(downloadUrlExpirationSeconds);
            URL presignedUrl = s3Template.createSignedGetURL(bucketName, document.getFilePath(), timeToLive);

            return presignedUrl.toString();
        } catch (Exception e) {
            log.error("Failed to generate S3 Pre-Signed URL for document ID: {}", documentId, e);
            throw new DmsUnavailableException("Unable to generate secure download link at this time.", e);
        }
    }
}