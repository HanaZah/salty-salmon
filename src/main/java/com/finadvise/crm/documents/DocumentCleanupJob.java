package com.finadvise.crm.documents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentCleanupJob {

    private final DocumentRepository documentRepository;
    private final S3Client s3Client;
    private final Clock clock;

    @Value("${app.storage.bucket-name}")
    private String bucketName;

    @Scheduled(cron = "${app.scheduling.cleanup.cron:0 0 2 * * ?}")
    public void runNightlyCleanup() {
        log.info("Starting nightly S3 cleanup job...");
        long startTime = System.currentTimeMillis();

        List<Document> pendingDeletions = documentRepository.findAllByStorageDeletedAtNullAndIsActiveFalse();

        if (pendingDeletions.isEmpty()) {
            log.info("No pending deletions found. Exiting cleanup job.");
            return;
        }

        int successCount = 0;

        for (Document document : pendingDeletions) {
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(document.getFilePath())
                        .build();
                s3Client.deleteObject(deleteRequest);

                document.setStorageDeletedAt(LocalDateTime.now(clock));
                documentRepository.save(document);

                successCount++;
            } catch (Exception e) {
                // If AWS fails, we log it and move on. The DB remains NULL,
                // so the job will automatically retry this file tomorrow.
                log.error("Failed to delete S3 object for document ID: {}", document.getId(), e);
            }
        }

        log.info("Cleanup complete in {}ms. Successfully purged {}/{} files from storage.",
                System.currentTimeMillis() - startTime, successCount, pendingDeletions.size());
    }
}
