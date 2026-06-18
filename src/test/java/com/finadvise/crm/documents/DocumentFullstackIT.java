package com.finadvise.crm.documents;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.common.Constants;
import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.products.*;
import com.finadvise.crm.users.Advisor;
import io.awspring.cloud.s3.S3Template;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class DocumentFullstackIT {

    @Container
    @ServiceConnection
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentTypeRepository documentTypeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private ProductTypeRepository productTypeRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;
    @Autowired private DocumentUploadOrchestrator orchestrator;

    // Infrastructure boundaries strictly mocked for HTTP/DB slice testing
    @MockitoBean private S3Template s3Template;
    @MockitoBean private Tika tika;

    private Client testClient;
    private DocumentType testDocumentType;

    @BeforeEach
    void setUp() {
        // Force synchronous execution to keep worker threads inside the @Transactional test boundary
        ReflectionTestUtils.setField(orchestrator, "ioExecutor", (Executor) Runnable::run);

        Advisor advisor = testFixtureFactory.getOrCreateTestAdvisor(
                501L, "ADV_501", "50150150", "DocAdvisor");
        testClient = testFixtureFactory.getOrCreateTestClient(
                501L, "CLI_501", "5015015015", "501501501", "Anderson", advisor);

        testDocumentType = documentTypeRepository.findByName("Contract")
                .orElseGet(() -> documentTypeRepository.save(DocumentType.builder().name("Contract").build()));
    }

    // --- 1. UPLOAD ENDPOINT TESTS ---

    @Test
    @WithMockUser(username = "ADV_501", authorities = "ADVISOR")
    void uploadDocuments_Success_CreatesNewDocuments() throws Exception {
        when(tika.detect(any(java.io.InputStream.class))).thenReturn("application/pdf");

        FileMetadataDTO meta = new FileMetadataDTO("signed_contract.pdf", testDocumentType.getId(), null);
        Map<String, FileMetadataDTO> metadataMap = Map.of("uuid-123", meta);

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(metadataMap)
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "files",
                "uuid-123" + Constants.DOCUMENT_UPLOAD_FILENAME_DELIMITER + "signed_contract.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "dummy pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/clients/{clientUid}/documents", testClient.getClientUid())
                        .file(metadataPart)
                        .file(filePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName").value("signed_contract.pdf"));

        List<Document> savedDocs = documentRepository.findAllByClientClientUidAndIsActiveTrue(testClient.getClientUid());
        assertThat(savedDocs).hasSize(1);
    }

    @Test
    @WithMockUser(username = "ROGUE_99", authorities = "ADVISOR")
    void uploadDocuments_Fails_WhenAdvisorDoesNotOwnClient() throws Exception {
        FileMetadataDTO meta = new FileMetadataDTO("rogue_file.pdf", testDocumentType.getId(), null);
        Map<String, FileMetadataDTO> metadataMap = Map.of("uuid-123", meta);

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(metadataMap));
        MockMultipartFile filePart = new MockMultipartFile(
                "files", "uuid-123" + Constants.DOCUMENT_UPLOAD_FILENAME_DELIMITER + "rogue_file.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy".getBytes());

        mockMvc.perform(multipart("/api/v1/clients/{clientUid}/documents", testClient.getClientUid())
                        .file(metadataPart)
                        .file(filePart))
                .andExpect(status().isNotFound());
    }

    // --- 2. GET ALL ENDPOINT TESTS ---

    @Test
    @WithMockUser(username = "ADV_501", authorities = "ADVISOR")
    void getClientDocuments_Success_ReturnsDocumentList() throws Exception {
        documentRepository.save(Document.builder()
                .fileName("identity.pdf")
                .filePath("s3-key-id")
                .documentType(testDocumentType)
                .client(testClient)
                .build());

        // Added pagination params and updated JSON paths for the new DTO wrapper
        mockMvc.perform(get("/api/v1/clients/{clientUid}/documents", testClient.getClientUid())
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientUid").value(testClient.getClientUid()))
                .andExpect(jsonPath("$.totalDocuments").value(1))
                .andExpect(jsonPath("$.documents.content", hasSize(1)))
                .andExpect(jsonPath("$.documents.content[0].fileName").value("identity.pdf"))
                .andExpect(jsonPath("$.documents.content[0].documentTypeName").value("Contract"));
    }

    // --- 3. GET SINGLE ENDPOINT TESTS ---

    @Test
    @WithMockUser(username = "ADV_501", authorities = "ADVISOR")
    void getDocumentById_Success_ReturnsSpecificDocument() throws Exception {
        Document savedDoc = documentRepository.save(Document.builder()
                .fileName("specific.pdf")
                .filePath("s3-key-specific")
                .documentType(testDocumentType)
                .client(testClient)
                .build());

        mockMvc.perform(get("/api/v1/clients/{clientUid}/documents/{docId}",
                        testClient.getClientUid(), savedDoc.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedDoc.getId()))
                .andExpect(jsonPath("$.fileName").value("specific.pdf"));
    }

    // --- 4. PATCH UPDATE ENDPOINT TESTS ---

    @Test
    @WithMockUser(username = "ADV_501", authorities = "ADVISOR")
    void updateDocument_Success_UpdatesMetadata() throws Exception {
        ProductType pType = productTypeRepository.save(ProductType.builder().name("TestProductType").build());
        Provider provider = providerRepository.save(Provider.builder().name("TestProductProvider").build());

        Product product = productRepository.save(Product.builder()
                .name("Savings Account")
                .amount(new java.math.BigDecimal("0"))
                .startDate(java.time.LocalDate.now())
                .nextAnniversary(java.time.LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(pType)
                .provider(provider)
                .build());

        Document savedDoc = documentRepository.save(Document.builder()
                .fileName("old_name.pdf")
                .filePath("s3-key-patch")
                .documentType(testDocumentType)
                .client(testClient)
                .build());

        DocumentUpdateRequestDTO payload = new DocumentUpdateRequestDTO("new_name.pdf", product.getId());

        mockMvc.perform(patch("/api/v1/clients/{clientUid}/documents/{docId}",
                        testClient.getClientUid(), savedDoc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("new_name.pdf"))
                .andExpect(jsonPath("$.productId").value(product.getId()));
    }

    @Test
    @WithMockUser(username = "ADV_501", authorities = "ADVISOR")
    void updateDocument_Fails_WithValidationErrorsOnBlankName() throws Exception {
        Document savedDoc = documentRepository.save(Document.builder()
                .fileName("valid.pdf")
                .filePath("s3-key-valid")
                .documentType(testDocumentType)
                .client(testClient)
                .build());

        DocumentUpdateRequestDTO payload = new DocumentUpdateRequestDTO("   ", null);

        mockMvc.perform(patch("/api/v1/clients/{clientUid}/documents/{docId}",
                        testClient.getClientUid(), savedDoc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // --- 5. PRE-SIGNED URL GENERATION TESTS ---

    @Test
    @WithMockUser(username = "ADV_501", authorities = "ADVISOR")
    void getDocumentDownloadLink_Success_ReturnsPreSignedUrl() throws Exception {
        Document savedDoc = documentRepository.save(Document.builder()
                .fileName("download.pdf")
                .filePath("s3-key-download")
                .documentType(testDocumentType)
                .client(testClient)
                .build());

        String expectedUrl = "https://mock-s3-url.com/download?signature=123";
        when(s3Template.createSignedGetURL(any(), eq("s3-key-download"), any(Duration.class)))
                .thenReturn(URI.create(expectedUrl).toURL());

        mockMvc.perform(get("/api/v1/clients/{clientUid}/documents/{docId}/download",
                        testClient.getClientUid(), savedDoc.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value(expectedUrl));
    }
}