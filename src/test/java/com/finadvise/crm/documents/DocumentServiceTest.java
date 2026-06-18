package com.finadvise.crm.documents;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.common.OwnershipValidator;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.products.Product;
import com.finadvise.crm.products.ProductRepository;
import io.awspring.cloud.s3.S3Template;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentTypeRepository documentTypeRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ProductRepository productRepository;
    @Mock private DocumentMapper documentMapper;
    @Mock private OwnershipValidator ownershipValidator;
    @Mock private S3Template s3Template;
    @Mock private Tika tika;
    @Mock private MinioS3BucketProvisioner bucketProvisioner;

    @Mock private MultipartFile mockFile;

    // Freeze time for deterministic assertions
    @Spy
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-06T10:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    private DocumentService documentService;

    private static final String BUCKET_NAME = "test-bucket";
    private static final long MAX_FILE_SIZE = 10485760L; // 10MB
    private static final long URL_EXPIRATION = 60L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentService, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(documentService, "maxFileSizeBytes", MAX_FILE_SIZE);
        ReflectionTestUtils.setField(documentService, "downloadUrlExpirationSeconds", URL_EXPIRATION);
    }

    // --- UPLOAD & SECURITY VALIDATION TESTS ---

    @Test
    void uploadDocument_ReturnsValidDto_WithDeterministicDate() throws Exception {
        String clientUid = "CLI_01";
        String employeeId = "EMP_01";
        FileMetadataDTO metadata = new FileMetadataDTO("test.pdf", 1L, null);

        when(ownershipValidator.canAccessClient(clientUid, employeeId)).thenReturn(true);
        when(documentTypeRepository.existsById(1L)).thenReturn(true);

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(tika.detect(any(InputStream.class))).thenReturn("application/pdf");

        DocumentUploadResultDTO result = documentService.uploadDocument(clientUid, employeeId, metadata, mockFile);

        assertNotNull(result);
        // Assert exactly against the fixed clock
        assertEquals(LocalDate.now(clock), result.uploadedAt());
        assertTrue(result.filePath().startsWith("clients/CLI_01/"));
    }

    @Test
    void uploadDocument_ThrowsResourceNotFound_WhenProductAccessDenied() {
        String clientUid = "CLI_01";
        String employeeId = "EMP_01";
        FileMetadataDTO metadata = new FileMetadataDTO("test.pdf", 1L, 99L);

        when(ownershipValidator.canAccessProduct(99L, clientUid, employeeId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                documentService.uploadDocument(clientUid, employeeId, metadata, mockFile)
        );
        verifyNoInteractions(s3Template);
    }

    @Test
    void validateFileSecurity_ThrowsUnsupportedFormat_WhenFileExceedsSizeLimit() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(MAX_FILE_SIZE + 1); // 1 byte over limit

        assertThrows(UnsupportedDocumentFormatException.class, () ->
                documentService.validateFileSecurity(mockFile)
        );
    }

    @Test
    void validateFileSecurity_ThrowsUnsupportedFormat_WhenMimeSpoofingDetected() throws Exception {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("safe_document.pdf");

        InputStream dummyStream = new ByteArrayInputStream(new byte[0]);
        when(mockFile.getInputStream()).thenReturn(dummyStream);

        when(tika.detect(any(InputStream.class))).thenReturn("application/x-msdownload");

        assertThrows(UnsupportedDocumentFormatException.class, () ->
                documentService.validateFileSecurity(mockFile)
        );
    }

    @Test
    void validateFileSecurity_AcceptsValidCsvDetectedAsTextPlain() throws Exception {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("data.csv");

        InputStream dummyStream = new ByteArrayInputStream(new byte[0]);
        when(mockFile.getInputStream()).thenReturn(dummyStream);

        when(tika.detect(any(InputStream.class))).thenReturn("text/plain");

        assertDoesNotThrow(() -> documentService.validateFileSecurity(mockFile));
    }

    // --- S3 SELF-HEALING TESTS ---

    private static class NoSuchBucketException extends RuntimeException {}

    @Test
    void uploadToS3WithSelfHealing_TriggersProvisionerAndRetries_WhenBucketMissing() {
        String safeKey = "clients/UID/file.pdf";
        InputStream stream = new ByteArrayInputStream(new byte[0]);

        RuntimeException awsException = new RuntimeException("Outer SDK Error", new NoSuchBucketException());

        doThrow(awsException).doReturn(null).when(s3Template).upload(BUCKET_NAME, safeKey, stream);

        assertDoesNotThrow(() -> documentService.uploadToS3WithSelfHealing(safeKey, stream));

        verify(s3Template, times(2)).upload(BUCKET_NAME, safeKey, stream);
        verify(bucketProvisioner, times(1)).ensureBucketExists();
    }

    // --- RETRIEVAL & UPDATE TESTS ---

    @Test
    void getDocumentById_ThrowsResourceNotFound_WhenDocumentIsInactive() {
        String clientUid = "CLI_01";
        String employeeId = "EMP_01";
        Long documentId = 1L;

        Document inactiveDoc = Document.builder().id(documentId).isActive(false).build();

        when(ownershipValidator.canAccessDocument(documentId, clientUid, employeeId)).thenReturn(true);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(inactiveDoc));

        assertThrows(ResourceNotFoundException.class, () ->
                documentService.getDocumentById(clientUid, documentId, employeeId)
        );
    }

    @Test
    void updateDocument_UpdatesFileNameAndProduct_Correctly() {
        String clientUid = "CLI_01";
        String employeeId = "EMP_01";
        Long documentId = 1L;
        Long newProductId = 200L;

        Client client = new Client();
        client.setClientUid(clientUid);

        Document existingDoc = Document.builder()
                .id(documentId)
                .fileName("OldName.pdf")
                .isActive(true)
                .client(client)
                .build();

        Product newProduct = new Product();
        newProduct.setId(newProductId);

        DocumentUpdateRequestDTO request = new DocumentUpdateRequestDTO("NewName.pdf", newProductId);

        when(ownershipValidator.canAccessDocument(documentId, clientUid, employeeId)).thenReturn(true);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(existingDoc));
        when(ownershipValidator.canAccessProduct(newProductId, clientUid, employeeId)).thenReturn(true);
        when(productRepository.getReferenceById(newProductId)).thenReturn(newProduct);
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArguments()[0]);

        documentService.updateDocument(clientUid, documentId, request, employeeId);

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());

        Document savedDoc = docCaptor.getValue();
        assertEquals("NewName.pdf", savedDoc.getFileName());
        assertNotNull(savedDoc.getProduct());
        assertEquals(newProductId, savedDoc.getProduct().getId());
    }

    // --- PRE-SIGNED URL TEST ---

    @Test
    void generateDownloadUrl_ReturnsPresignedUrl_Optimistically() throws Exception {
        String clientUid = "CLI_01";
        String employeeId = "EMP_01";
        Long documentId = 1L;

        Document doc = Document.builder().id(documentId).filePath("s3-key-123").build();
        URL mockUrl = URI.create("https://s3.aws.com/test-bucket/s3-key-123?signature=xyz").toURL();

        when(ownershipValidator.canAccessDocument(documentId, clientUid, employeeId)).thenReturn(true);
        when(documentRepository.findByIdAndIsActiveTrue(documentId)).thenReturn(Optional.of(doc));
        when(s3Template.createSignedGetURL(eq(BUCKET_NAME), eq("s3-key-123"), any(java.time.Duration.class)))
                .thenReturn(mockUrl);

        String result = documentService.generateDownloadUrl(clientUid, documentId, employeeId);

        assertEquals(mockUrl.toString(), result);
        verify(s3Template, never()).objectExists(anyString(), anyString());
    }
}