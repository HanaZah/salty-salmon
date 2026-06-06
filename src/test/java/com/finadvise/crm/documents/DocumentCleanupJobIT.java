package com.finadvise.crm.documents;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.users.Advisor;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class DocumentCleanupJobIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private DocumentCleanupJob cleanupJob;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentTypeRepository documentTypeRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;

    @MockitoBean private S3Client s3Client;

    private Client testClient;
    private DocumentType documentType;

    @BeforeEach
    void setUp() {
        Advisor advisor = testFixtureFactory.getOrCreateTestAdvisor(
                301L, "EMP-0301", "30000001", "CleanAdvisor");
        testClient = testFixtureFactory.getOrCreateTestClient(
                301L, "CLI-0301", "3000000001", "300000001", "CleanClient", advisor);
        documentType = documentTypeRepository.save(DocumentType.builder().name("ID").build());
    }

    @Test
    void runNightlyCleanup_ProcessesOnlyPendingDeletions() {
        // 1. Active document (Should be ignored)
        documentRepository.save(Document.builder()
                .fileName("active.pdf").filePath("key-1").isActive(true).storageDeletedAt(null)
                .documentType(documentType).client(testClient).build());

        // 2. Already processed document (Should be ignored)
        documentRepository.save(Document.builder()
                .fileName("purged.pdf").filePath("key-2").isActive(false).storageDeletedAt(LocalDateTime.now())
                .documentType(documentType).client(testClient).build());

        // 3. Pending deletion (Target)
        Document target = documentRepository.save(Document.builder()
                .fileName("pending.pdf").filePath("key-3").isActive(false).storageDeletedAt(null)
                .documentType(documentType).client(testClient).build());

        cleanupJob.runNightlyCleanup();

        // Verify S3 delete was called exactly once for the target
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));

        // Verify database state change
        Document processedTarget = documentRepository.findById(target.getId()).orElseThrow();
        assertThat(processedTarget.getStorageDeletedAt()).isNotNull();
    }
}
