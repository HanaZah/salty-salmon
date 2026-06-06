package com.finadvise.crm.documents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentCleanupJobTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private S3Client s3Client;

    @Spy
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-06T02:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    private DocumentCleanupJob cleanupJob;

    private static final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cleanupJob, "bucketName", BUCKET_NAME);
    }

    @Test
    void runNightlyCleanup_ExitsEarly_WhenNoPendingDeletions() {
        when(documentRepository.findAllByStorageDeletedAtNullAndIsActiveFalse()).thenReturn(Collections.emptyList());

        cleanupJob.runNightlyCleanup();

        verifyNoInteractions(s3Client);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void runNightlyCleanup_ProcessesDeletionsAndUpdatesTimestamp_Correctly() {
        Document doc1 = Document.builder().id(1L).filePath("s3-key-1").build();
        Document doc2 = Document.builder().id(2L).filePath("s3-key-2").build();

        when(documentRepository.findAllByStorageDeletedAtNullAndIsActiveFalse()).thenReturn(List.of(doc1, doc2));

        cleanupJob.runNightlyCleanup();

        verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, times(2)).save(docCaptor.capture());

        List<Document> savedDocs = docCaptor.getAllValues();
        assertEquals(LocalDateTime.now(clock), savedDocs.get(0).getStorageDeletedAt());
        assertEquals(LocalDateTime.now(clock), savedDocs.get(1).getStorageDeletedAt());
    }

    @Test
    void runNightlyCleanup_ContinuesProcessing_WhenS3ThrowsException() {
        Document successDoc = Document.builder().id(1L).filePath("s3-key-success").build();
        Document failDoc = Document.builder().id(2L).filePath("s3-key-fail").build();

        when(documentRepository.findAllByStorageDeletedAtNullAndIsActiveFalse()).thenReturn(List.of(failDoc, successDoc));

        // Force S3 to throw an exception on the first file, but succeed on the second
        doThrow(S3Exception.builder().message("Access Denied").build())
                .when(s3Client).deleteObject(argThat((DeleteObjectRequest req) -> req.key().equals("s3-key-fail")));

        cleanupJob.runNightlyCleanup();

        // S3 client should still be called twice despite the first failure
        verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));

        // The repository should only save the document that successfully deleted from S3
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, times(1)).save(docCaptor.capture());

        Document savedDoc = docCaptor.getValue();
        assertEquals(1L, savedDoc.getId()); // Only doc1 was saved
        assertNotNull(savedDoc.getStorageDeletedAt());
        assertNull(failDoc.getStorageDeletedAt()); // Failed doc remains null
    }
}
