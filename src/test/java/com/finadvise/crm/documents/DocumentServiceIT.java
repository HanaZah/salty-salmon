package com.finadvise.crm.documents;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.common.ResourceNotFoundException;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class DocumentServiceIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private DocumentService documentService;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentTypeRepository documentTypeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private ProductTypeRepository productTypeRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;

    // Isolate S3 and Tika from the DB integration test
    @MockitoBean private S3Template s3Template;
    @MockitoBean private Tika tika;
    @MockitoBean private S3BucketProvisioner bucketProvisioner;

    private Advisor testAdvisor;
    private Client testClient;
    private DocumentType documentType;

    @BeforeEach
    void setUp() {
        testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                201L, "EMP-0201", "20000001", "DocAdvisor");
        testClient = testFixtureFactory.getOrCreateTestClient(
                201L, "CLI-0201", "2000000001", "200000001", "DocClient", testAdvisor);

        documentType = documentTypeRepository.save(DocumentType.builder().name("Contract").build());
    }

    @Test
    void saveAllDocuments_PersistsToDatabase_Successfully() {
        FileMetadataDTO meta = new FileMetadataDTO("contract.pdf", documentType.getId(), null);
        DocumentUploadResultDTO uploadResult = new DocumentUploadResultDTO("s3-key-1", LocalDate.now(), meta);

        List<DocumentDTO> results = documentService.saveAllDocuments(testClient.getId(), List.of(uploadResult));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().id()).isNotNull();
        assertThat(results.getFirst().fileName()).isEqualTo("contract.pdf");

        Document savedDoc = documentRepository.findById(results.getFirst().id()).orElseThrow();
        assertThat(savedDoc.getFilePath()).isEqualTo("s3-key-1");
        assertThat(savedDoc.getClient().getId()).isEqualTo(testClient.getId());
    }

    @Test
    void updateDocument_UpdatesMetadata_WhenAuthorized() {
        ProductType pType = productTypeRepository.save(ProductType.builder().name("Insurance").build());
        Provider provider = providerRepository.save(Provider.builder().name("Allianz").build());

        Product product = productRepository.save(Product.builder()
                .name("Life Ins")
                .amount(new java.math.BigDecimal("1000.00"))
                .startDate(LocalDate.now())
                .nextAnniversary(LocalDate.now().plusYears(1))
                .productType(pType)
                .provider(provider)
                .client(testClient)
                .build());

        Document doc = documentRepository.save(Document.builder()
                .fileName("old.pdf")
                .filePath("s3-key-2")
                .documentType(documentType)
                .client(testClient)
                .build());

        DocumentUpdateRequestDTO request = new DocumentUpdateRequestDTO("new.pdf", product.getId());

        DocumentDTO result = documentService.updateDocument(
                testClient.getClientUid(), doc.getId(), request, testAdvisor.getEmployeeId());

        assertThat(result.fileName()).isEqualTo("new.pdf");
        assertThat(result.productId()).isEqualTo(product.getId());
    }

    @Test
    void updateDocument_ThrowsResourceNotFound_OnUnauthorizedAccess() {
        Advisor rogueAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                202L, "EMP-0202", "20000002", "Rogue");

        Document doc = documentRepository.save(Document.builder()
                .fileName("old.pdf")
                .filePath("s3-key-3")
                .documentType(documentType)
                .client(testClient)
                .build());

        DocumentUpdateRequestDTO request = new DocumentUpdateRequestDTO("new.pdf", null);

        assertThatThrownBy(() -> documentService.updateDocument(
                testClient.getClientUid(), doc.getId(), request, rogueAdvisor.getEmployeeId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}