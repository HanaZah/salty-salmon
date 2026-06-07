package com.finadvise.crm.documents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("test") // ONLY active during integration tests
public class NoOpS3BucketProvisioner implements S3BucketProvisioner {

    @Override
    public void ensureBucketExists() {
        log.debug("No-Op S3 provisioner invoked. Bypassing MinIO network call for test environment.");
    }
}
