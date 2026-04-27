package com.finadvise.crm.products;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnniversaryRolloverJob {

    private final ProductRepository productRepository;
    private final AnniversaryProcessor anniversaryProcessor;

    private static final int CHUNK_SIZE = 500;

    // Runs every day at 01:00 AM server time
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void rollForwardAnniversaries() {
        log.info("Starting nightly product anniversary rollover job...");
        LocalDate today = LocalDate.now();
        int processedCount = 0;

        while (true) {
            Slice<Product> slice = productRepository.findByNextAnniversaryLessThan(
                    today, PageRequest.of(0, CHUNK_SIZE)
            );

            if (slice.isEmpty()) {
                break;
            }

            anniversaryProcessor.processChunk(slice.getContent());
            processedCount += slice.getNumberOfElements();

            log.debug("Processed chunk of {}, total so far: {}", slice.getNumberOfElements(), processedCount);
        }
        log.info("Completed nightly rollover. Total products updated: {}", processedCount);
    }
}
