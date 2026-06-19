package com.finadvise.crm.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayRolloverJob {

    private final ClientRepository clientRepository;
    private final BirthdayProcessor birthdayProcessor;
    private final Clock clock;

    private static final int CHUNK_SIZE = 500;

    // Runs every day at 02:00 AM server time
    @Scheduled(cron = "${app.scheduling.birthday-rollover.cron:0 0 2 * * ?}")
    @Transactional
    public void rollForwardBirthdays() {
        log.info("Starting nightly client birthday rollover job...");
        LocalDate today = LocalDate.now(clock);
        int processedCount = 0;

        while (true) {
            // We only process active clients to save resources
            Slice<Client> slice = clientRepository.findByNextBirthdayLessThanAndIsActiveTrue(
                    today, PageRequest.of(0, CHUNK_SIZE)
            );

            if (slice.isEmpty()) {
                break;
            }

            birthdayProcessor.processChunk(slice.getContent());
            processedCount += slice.getNumberOfElements();

            log.debug("Processed chunk of {}, total so far: {}", slice.getNumberOfElements(), processedCount);
        }
        log.info("Completed nightly rollover. Total active clients updated: {}", processedCount);
    }
}
