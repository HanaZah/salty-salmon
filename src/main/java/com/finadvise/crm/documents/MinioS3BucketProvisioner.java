package com.finadvise.crm.documents;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile( "!test")
public class MinioS3BucketProvisioner implements S3BucketProvisioner {

    private final S3Template s3Template;

    @Value("${app.storage.bucket-name}")
    private String bucketName;

    /**
     * Ensures the bucket exists when the application starts.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeStorage() {
        log.info("Application ready. Proceeding to DMS S3 bucket verification...");
        ensureBucketExists();
    }

    /**
     * Ensures the bucket exists. If MinIO is down, this will throw an exception
     * that the GlobalExceptionHandler can catch, but it won't crash the app.
     */
    public synchronized void ensureBucketExists() {
        log.info("Verifying MinIO bucket '{}' exists...", bucketName);
        try {
            if (!s3Template.bucketExists(bucketName)) {
                log.info("Bucket does not exist. Creating '{}'...", bucketName);
                s3Template.createBucket(bucketName);
            }
            log.info("Bucket '{}' is ready for operations.", bucketName);
        } catch (Exception e) {
            log.error("Failed to connect to MinIO or create bucket. DMS is currently unavailable.", e);
            throw new DmsUnavailableException("Document Management System is currently unavailable. Please try uploading later.");
        }
    }

}
