package com.finadvise.crm.documents;

import com.finadvise.crm.clients.ClientService;
import com.finadvise.crm.common.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentUploadOrchestratorConcurrencyTest {

    @Mock private DocumentService documentService;
    @Mock private ClientService clientService;
    @Mock private MultipartFile mockFile;

    private DocumentUploadOrchestrator orchestrator;
    private ExecutorService realThreadPool;

    @BeforeEach
    void setUp() {
        orchestrator = new DocumentUploadOrchestrator(documentService, clientService, null);

        realThreadPool = Executors.newFixedThreadPool(4);
        ReflectionTestUtils.setField(orchestrator, "ioExecutor", realThreadPool);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Clean up the thread pool after the test to prevent memory leaks in the test suite
        realThreadPool.shutdown();
        realThreadPool.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void uploadDocumentsBatch_ExecutesInParallel_ProvingTrueConcurrency() {
        String clientUid = "CLI_01";
        String employeeId = "EMP_01";
        when(clientService.getClientId(clientUid)).thenReturn(100L);

        when(mockFile.getOriginalFilename()).thenReturn("uuid" + Constants.DOCUMENT_UPLOAD_FILENAME_DELIMITER + "test.pdf");
        FileMetadataDTO meta = new FileMetadataDTO("test.pdf", 1L, null);
        Map<String, FileMetadataDTO> metadataMap = Map.of("uuid", meta);
        List<MultipartFile> fourFiles = List.of(mockFile, mockFile, mockFile, mockFile);

        // We force every upload to take exactly 500ms.
        when(documentService.uploadDocument(any(), any(), any(), any())).thenAnswer(invocation -> {
            Thread.sleep(500);
            return new DocumentUploadResultDTO("key", LocalDate.now(), meta);
        });

        long startTime = System.currentTimeMillis();
        orchestrator.uploadDocumentsBatch(clientUid, metadataMap, fourFiles, employeeId);
        long duration = System.currentTimeMillis() - startTime;

        // If the code was running sequentially, 4 files * 500ms = 2000ms absolute minimum.
        // Because we have a 4-thread pool, they should all process at the same time (roughly ~500ms plus overhead).
        assertThat(duration)
                .as("Execution took too long, indicating tasks did not run in parallel on the thread pool")
                .isLessThan(1000);

        verify(documentService, times(4)).uploadDocument(any(), any(), any(), any());
    }
}
