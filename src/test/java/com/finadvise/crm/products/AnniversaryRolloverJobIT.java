package com.finadvise.crm.products;

import com.finadvise.crm.clients.Client;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.scheduling.enabled=true",  // Making sure the job scheduling is enabled ONLY for this test
        "app.scheduling.anniversary-rollover.cron=*/1 * * * * *"
})
@Testcontainers
@ActiveProfiles("test")
class AnniversaryRolloverJobIT {

    @Container
    @ServiceConnection
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductTypeRepository productTypeRepository;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;

    private Client testClient;
    private ProductType testType;
    private Provider testProvider;

    @BeforeEach
    void setUp() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                601L, "ADV_601", "60160160", "ProdAdvisor"
        );
        testClient = testFixtureFactory.getOrCreateTestClient(
                601L, "CLI_601", "6016016016", "601601601", "Smith", testAdvisor);

        testType = productTypeRepository.save(ProductType.builder().name("TestProductType").build());
        testProvider = providerRepository.save(Provider.builder().name("TestProductProvider").build());
    }

    @AfterEach
    void tearDown() {
        // Required because this class cannot be @Transactional (background threads need to see committed data)
        productRepository.deleteAll();
        productTypeRepository.deleteAll();
        providerRepository.deleteAll();
    }

    @Test
    void rollForwardAnniversaries_UpdatesDatesAndSavesToDatabase() {
        LocalDate today = LocalDate.now();
        LocalDate pastDueOriginalDate = today.minusDays(5);
        LocalDate futureOriginalDate = today.plusMonths(11);

        Product pastDueProduct = productRepository.save(Product.builder()
                .name("Old Policy")
                .amount(new BigDecimal("5000.00"))
                .startDate(today.minusYears(2))
                .nextAnniversary(pastDueOriginalDate) // Eligible
                .productType(testType)
                .provider(testProvider)
                .client(testClient)
                .build());

        Product futureProduct = productRepository.save(Product.builder()
                .name("New Policy")
                .amount(new BigDecimal("10000.00"))
                .startDate(today.minusMonths(1))
                .nextAnniversary(futureOriginalDate) // NOT eligible
                .productType(testType)
                .provider(testProvider)
                .client(testClient)
                .build());

        // The background cron thread will fire, read, update, and flush to the DB.
        // We wait and poll the DB to assert the final state.
        Awaitility.await()
                .atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> {
                    Product updatedPastDue = productRepository.findById(pastDueProduct.getId()).orElseThrow();
                    Product unchangedFuture = productRepository.findById(futureProduct.getId()).orElseThrow();

                    assertThat(updatedPastDue.getNextAnniversary()).isEqualTo(pastDueOriginalDate.plusYears(1));
                    assertThat(unchangedFuture.getNextAnniversary()).isEqualTo(futureOriginalDate);
                });
    }
}