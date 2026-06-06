package com.finadvise.crm.documents;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.common.Constants;
import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.users.Advisor;
import io.awspring.cloud.s3.S3Template;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class DocumentUploadOrchestratorIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private DocumentUploadOrchestrator orchestrator;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentTypeRepository documentTypeRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;

    @MockitoBean private S3Template s3Template;
    @MockitoBean private Tika tika;

    private Advisor testAdvisor;
    private Client testClient;
    private DocumentType documentType;

    @BeforeEach
    void setUp() {
        // Force synchronous execution to keep database calls within the test's ThreadLocal transaction
        ReflectionTestUtils.setField(orchestrator, "ioExecutor", (Executor) Runnable::run);

        testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                401L, "EMP-0401", "40000001", "OrchAdvisor");
        testClient = testFixtureFactory.getOrCreateTestClient(
                401L, "CLI-0401", "4000000001", "400000001", "OrchClient", testAdvisor);
        documentType = documentTypeRepository.findByName("Report").orElseGet(
                () -> documentTypeRepository.save(DocumentType.builder().name("Report").build())
        );
    }

    @Test
    void uploadDocumentsBatch_SavesAllFilesConcurrently() throws Exception {
        // Mock Tika to bypass security analysis for test speed
        when(tika.detect(any(java.io.InputStream.class))).thenReturn("application/pdf");

        FileMetadataDTO meta1 = new FileMetadataDTO("file1.pdf", documentType.getId(), null);
        FileMetadataDTO meta2 = new FileMetadataDTO("file2.pdf", documentType.getId(), null);

        MultipartFile file1 = new MockMultipartFile(
                "files", "uuid1" + Constants.DOCUMENT_UPLOAD_FILENAME_DELIMITER + "file1.pdf",
                "application/pdf", "dummy".getBytes());
        MultipartFile file2 = new MockMultipartFile(
                "files", "uuid2" + Constants.DOCUMENT_UPLOAD_FILENAME_DELIMITER + "file2.pdf",
                "application/pdf", "dummy".getBytes());

        Map<String, FileMetadataDTO> metadataMap = Map.of("uuid1", meta1, "uuid2", meta2);

        List<DocumentDTO> results = orchestrator.uploadDocumentsBatch(
                testClient.getClientUid(), metadataMap, List.of(file1, file2), testAdvisor.getEmployeeId());

        assertThat(results).hasSize(2);
        assertThat(documentRepository.findAllByClientClientUidAndIsActiveTrue(testClient.getClientUid())).hasSize(2);
    }
}