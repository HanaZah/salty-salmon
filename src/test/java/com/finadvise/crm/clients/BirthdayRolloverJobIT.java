package com.finadvise.crm.clients;

import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.users.Advisor;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.scheduling.enabled=true",
        "app.scheduling.birthday-rollover.cron=*/1 * * * * *"
})
@Testcontainers
@ActiveProfiles("test")
class BirthdayRolloverJobIT {

    @Container
    @ServiceConnection
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private ClientRepository clientRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;

    private Client pastDueActiveClient;
    private Client futureActiveClient;
    private Client pastDueInactiveClient;

    @BeforeEach
    void setUp() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                701L, "ADV_701", "70170170", "JobAdvisor"
        );

        LocalDate today = LocalDate.now();

        // 1. Active, Birthday passed -> SHOULD UPDATE
        pastDueActiveClient = testFixtureFactory.getOrCreateTestClient(
                701L, "CLI_701", "7017017011", "701701701", "PastDue", testAdvisor);
        pastDueActiveClient.setBirthDate(today.minusYears(30).minusDays(5));
        pastDueActiveClient.setNextBirthday(today.minusDays(5));
        pastDueActiveClient.setActive(true);
        pastDueActiveClient = clientRepository.save(pastDueActiveClient);

        // 2. Active, Birthday in future -> SHOULD NOT UPDATE
        futureActiveClient = testFixtureFactory.getOrCreateTestClient(
                702L, "CLI_702", "7017017012", "701701702", "Future", testAdvisor);
        futureActiveClient.setBirthDate(today.minusYears(25).plusMonths(2));
        futureActiveClient.setNextBirthday(today.plusMonths(2));
        futureActiveClient.setActive(true);
        futureActiveClient = clientRepository.save(futureActiveClient);

        // 3. Inactive, Birthday passed -> SHOULD NOT UPDATE
        pastDueInactiveClient = testFixtureFactory.getOrCreateTestClient(
                703L, "CLI_703", "7017017013", "701701703", "Inactive", testAdvisor);
        pastDueInactiveClient.setBirthDate(today.minusYears(40).minusDays(10));
        pastDueInactiveClient.setNextBirthday(today.minusDays(10));
        pastDueInactiveClient.setActive(false);
        pastDueInactiveClient = clientRepository.save(pastDueInactiveClient);
    }

    @AfterEach
    void tearDown() {
        clientRepository.deleteAll();
    }

    @Test
    void rollForwardBirthdays_UpdatesDatesOnlyForActiveClientsWithPastBirthdays() {
        LocalDate today = LocalDate.now();
        LocalDate expectedNewBirthday = pastDueActiveClient.getBirthDate().withYear(today.getYear() + 1);

        Awaitility.await()
                .atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> {
                    Client updatedActive = clientRepository.findById(pastDueActiveClient.getId()).orElseThrow();
                    Client unchangedFuture = clientRepository.findById(futureActiveClient.getId()).orElseThrow();
                    Client unchangedInactive = clientRepository.findById(pastDueInactiveClient.getId()).orElseThrow();

                    assertThat(updatedActive.getNextBirthday()).isEqualTo(expectedNewBirthday);
                    assertThat(unchangedFuture.getNextBirthday()).isEqualTo(futureActiveClient.getNextBirthday());
                    assertThat(unchangedInactive.getNextBirthday()).isEqualTo(pastDueInactiveClient.getNextBirthday());
                });
    }
}
