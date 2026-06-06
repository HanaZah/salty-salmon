package com.finadvise.crm.documents;

import com.finadvise.crm.clients.ClientService;
import com.finadvise.crm.common.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentUploadOrchestratorTest {

    @Mock private DocumentService documentService;
    @Mock private ClientService clientService;
    @Mock private MultipartFile mockFile1;
    @Mock private MultipartFile mockFile2;

    @InjectMocks
    private DocumentUploadOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        // Force synchronous execution for deterministic testing without thread sleeps
        ReflectionTestUtils.setField(orchestrator, "ioExecutor", (Executor) Runnable::run);
    }

    @Test
    void uploadDocumentsBatch_ProcessesAndPairsCorrectly() {
        String clientUid = "CLI_01";
        String employeeId = "EMP_01";
        Long clientId = 100L;

        when(clientService.getClientId(clientUid)).thenReturn(clientId);

        // Dynamically build the filename using the actual constant
        when(mockFile1.getOriginalFilename()).thenReturn("uuid1" + Constants.DOCUMENT_UPLOAD_FILENAME_DELIMITER + "contract.pdf");
        when(mockFile2.getOriginalFilename()).thenReturn("uuid2" + Constants.DOCUMENT_UPLOAD_FILENAME_DELIMITER + "id_card.jpg");

        FileMetadataDTO meta1 = new FileMetadataDTO("contract.pdf", 1L, null);
        FileMetadataDTO meta2 = new FileMetadataDTO("id_card.jpg", 2L, null);
        Map<String, FileMetadataDTO> metadataMap = Map.of("uuid1", meta1, "uuid2", meta2);

        DocumentUploadResultDTO result1 = new DocumentUploadResultDTO("key1", LocalDate.now(), meta1);
        DocumentUploadResultDTO result2 = new DocumentUploadResultDTO("key2", LocalDate.now(), meta2);

        when(documentService.uploadDocument(clientUid, employeeId, meta1, mockFile1)).thenReturn(result1);
        when(documentService.uploadDocument(clientUid, employeeId, meta2, mockFile2)).thenReturn(result2);

        orchestrator.uploadDocumentsBatch(clientUid, metadataMap, List.of(mockFile1, mockFile2), employeeId);

        verify(documentService).saveAllDocuments(eq(clientId), argThat(list -> list.size() == 2));
    }

    @Test
    void pairFilesWithMetadata_ThrowsException_WhenDelimiterMissing() {
        String clientUid = "CLI_01";
        Map<String, FileMetadataDTO> metadataMap = Map.of("uuid1", new FileMetadataDTO("test.pdf", 1L, null));

        // Malformed name strictly lacking the delimiter
        when(mockFile1.getOriginalFilename()).thenReturn("uuid1-test.pdf");

        assertThrows(MalformedDocumentPayloadException.class, () ->
                orchestrator.uploadDocumentsBatch(clientUid, metadataMap, List.of(mockFile1), "EMP_01")
        );

        verifyNoInteractions(documentService);
    }

    @Test
    void pairFilesWithMetadata_ThrowsException_WhenMetadataIsMissing() {
        String clientUid = "CLI_01";
        Map<String, FileMetadataDTO> metadataMap = Map.of();

        when(mockFile1.getOriginalFilename()).thenReturn("uuid1" + Constants.DOCUMENT_UPLOAD_FILENAME_DELIMITER + "test.pdf");

        assertThrows(MalformedDocumentPayloadException.class, () ->
                orchestrator.uploadDocumentsBatch(clientUid, metadataMap, List.of(mockFile1), "EMP_01")
        );
    }

    @Test
    void uploadDocumentsBatch_UnwrapsConcurrencyExceptions_Correctly() {
        String clientUid = "CLI_01";
        String employeeId = "EMP_01";

        when(clientService.getClientId(clientUid)).thenReturn(100L);
        when(mockFile1.getOriginalFilename()).thenReturn("uuid1" + Constants.DOCUMENT_UPLOAD_FILENAME_DELIMITER + "test.pdf");

        FileMetadataDTO meta1 = new FileMetadataDTO("test.pdf", 1L, null);
        Map<String, FileMetadataDTO> metadataMap = Map.of("uuid1", meta1);

        DmsUnavailableException coreException = new DmsUnavailableException("S3 is down");
        when(documentService.uploadDocument(any(), any(), any(), any())).thenThrow(coreException);

        DocumentBatchProcessingException thrown = assertThrows(DocumentBatchProcessingException.class, () ->
                orchestrator.uploadDocumentsBatch(clientUid, metadataMap, List.of(mockFile1), employeeId)
        );

        assertEquals(coreException, thrown.getCause());
    }
}