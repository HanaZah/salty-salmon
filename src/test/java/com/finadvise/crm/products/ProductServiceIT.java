package com.finadvise.crm.products;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.users.Advisor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ProductServiceIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductTypeRepository productTypeRepository;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;
    @Autowired private ClientRepository clientRepository;

    // --- CREATE TESTS ---

    @Test
    void createProduct_SavesSuccessfully_WhenAdvisorOwnsClient() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                101L, "EMP-0101", "10000001", "ValidAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                101L, "CLI-0101", "1000000001", "100000001", "Smith", testAdvisor);

        ProductType type = productTypeRepository.save(ProductType.builder().name("Life Insurance").build());
        Provider provider = providerRepository.save(Provider.builder().name("Allianz").build());

        ProductDTO payload = new ProductDTO(
                null, "Premium Life", new BigDecimal("1500.00"),
                LocalDate.now(), null, type.getId(), null, provider.getId(), null, null
        );

        ProductDTO result = productService.createProduct(testClient.getClientUid(), payload, testAdvisor.getEmployeeId());

        assertThat(result.id()).isNotNull();
        assertThat(result.name()).isEqualTo("Premium Life");

        Product savedProduct = productRepository.findById(result.id()).orElseThrow();
        assertThat(savedProduct.getClient().getId()).isEqualTo(testClient.getId());
    }

    @Test
    void createProduct_ThrowsAccessDeniedException_WhenAdvisorDoesNotOwnClient() {
        Advisor primaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                102L, "EMP-0102", "10000002", "OwnerAdvisor");
        Advisor rogueAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                103L, "EMP-0103", "10000003", "RogueAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                102L, "CLI-0102", "1000000002", "100000002", "Jones", primaryAdvisor);

        ProductType type = productTypeRepository.save(ProductType.builder().name("Pension").build());
        Provider provider = providerRepository.save(Provider.builder().name("Generali").build());

        ProductDTO payload = new ProductDTO(
                null, "Basic Pension", new BigDecimal("500.00"),
                LocalDate.now(), null, type.getId(), null, provider.getId(), null, null
        );

        assertThatThrownBy(() -> productService.createProduct(testClient.getClientUid(), payload, rogueAdvisor.getEmployeeId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Client not found or access denied");
    }

    // --- UPDATE TESTS ---

    @Test
    void updateProduct_UpdatesSuccessfully_WhenAdvisorOwnsClient() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                104L, "EMP-0104", "10000004", "OwnerAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                104L, "CLI-0104", "1000000002", "100000004", "Williams", testAdvisor);

        ProductType type = productTypeRepository.save(ProductType.builder().name("Investment").build());
        Provider provider = providerRepository.save(Provider.builder().name("Amundi").build());

        Product existingProduct = productRepository.save(Product.builder()
                .name("Global Equity")
                .amount(new BigDecimal("10000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(type)
                .provider(provider)
                .build());

        ProductDTO updatePayload = new ProductDTO(
                existingProduct.getId(), "Updated Equity", new BigDecimal("15000.00"),
                LocalDate.now(), null, type.getId(), null, provider.getId(), null, null
        );

        ProductDTO result = productService.updateProduct(testClient.getClientUid(), existingProduct.getId(), updatePayload, testAdvisor.getEmployeeId());

        assertThat(result.name()).isEqualTo("Updated Equity");
        assertThat(result.amount()).isEqualByComparingTo("15000.00");
    }

    @Test
    void updateProduct_ThrowsResourceNotFoundException_ToPreventIdEnumeration() {
        Advisor primaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                105L, "EMP-0105", "10000005", "OwnerAdvisor");
        Advisor rogueAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                106L, "EMP-0106", "10000006", "RogueAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                105L, "CLI-0105", "1000000005", "100000005", "Brown", primaryAdvisor);

        ProductType type = productTypeRepository.save(ProductType.builder().name("Savings").build());
        Provider provider = providerRepository.save(Provider.builder().name("Erste").build());

        Product existingProduct = productRepository.save(Product.builder()
                .name("Kids Savings")
                .amount(new BigDecimal("5000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(type)
                .provider(provider)
                .build());

        ProductDTO updatePayload = new ProductDTO(
                existingProduct.getId(), "Hacked Name", new BigDecimal("0.00"),
                LocalDate.now(), null, type.getId(), null, provider.getId(), null, null
        );

        assertThatThrownBy(() -> productService.updateProduct(
                testClient.getClientUid(), existingProduct.getId(), updatePayload, rogueAdvisor.getEmployeeId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found, does not belong to this client, or access denied");
    }

    // --- DELETE TESTS ---

    @Test
    void deleteProduct_DeletesSuccessfully_WhenAdvisorOwnsClient() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                107L, "EMP-0107", "10000007", "OwnerAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                107L, "CLI-0107", "1000000007", "100000007", "Miller", testAdvisor);

        ProductType type = productTypeRepository.save(ProductType.builder().name("Bonds").build());
        Provider provider = providerRepository.save(Provider.builder().name("State").build());

        Product product = productRepository.save(Product.builder()
                .name("Gov Bonds")
                .amount(new BigDecimal("20000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(type)
                .provider(provider)
                .build());

        productService.deleteProduct(testClient.getClientUid(), product.getId(), testAdvisor.getEmployeeId());

        assertThat(productRepository.findById(product.getId())).isEmpty();
    }

    @Test
    void deleteProduct_ThrowsResourceNotFoundException_ToPreventIdEnumeration() {
        Advisor primaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                108L, "EMP-0108", "10000008", "OwnerAdvisor");
        Advisor rogueAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                109L, "EMP-0109", "10000009", "RogueAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                108L, "CLI-0108", "1000000008", "100000008", "Taylor", primaryAdvisor);

        ProductType type = productTypeRepository.save(ProductType.builder().name("Mutual Fund").build());
        Provider provider = providerRepository.save(Provider.builder().name("Fidelity").build());

        Product product = productRepository.save(Product.builder()
                .name("Tech Fund")
                .amount(new BigDecimal("30000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(type)
                .provider(provider)
                .build());

        assertThatThrownBy(() -> productService.deleteProduct(
                testClient.getClientUid(), product.getId(), rogueAdvisor.getEmployeeId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found, does not belong to this client, or access denied");
    }

    // --- GET CLIENT PRODUCTS TESTS ---

    @Test
    void getClientProducts_ReturnsAllProducts_WhenPrimaryAdvisor() {
        Advisor primaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                110L, "EMP-0110", "10000010", "Primary");
        Advisor secondaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                111L, "EMP-0111", "10000011", "Secondary");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                110L, "CLI-0110", "1000000010", "100000010", "Anderson", primaryAdvisor);

        ProductType type = productTypeRepository.save(ProductType.builder().name("ETF").build());
        Provider provider = providerRepository.save(Provider.builder().name("Vanguard").build());

        // Product 1: No specific manager (defaults to primary)
        productRepository.save(Product.builder()
                .name("S&P 500")
                .amount(new BigDecimal("10000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(type)
                .provider(provider)
                .build());

        testClient.setAdvisor(secondaryAdvisor);
        clientRepository.save(testClient);
        clientRepository.flush();

        // Product 2: Managed explicitly by the secondary advisor
        productRepository.save(Product.builder()
                .name("Emerging Markets")
                .amount(new BigDecimal("5000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(type)
                .provider(provider)
                .managedBy(secondaryAdvisor)
                .build());

        testClient.setAdvisor(primaryAdvisor);
        clientRepository.save(testClient);
        clientRepository.flush();

        ClientProductsDTO result = productService.getClientProducts(testClient.getClientUid(), primaryAdvisor.getEmployeeId());

        assertThat(result.products()).hasSize(2);
        assertThat(result.totalActive()).isEqualTo(2);
    }

    @Test
    void getClientProducts_ReturnsOnlyManagedProducts_WhenSecondaryAdvisor() {
        Advisor primaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                112L, "EMP-0112", "10000012", "Primary");
        Advisor secondaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                113L, "EMP-0113", "10000013", "Secondary");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                112L, "CLI-0112", "1000000012", "100000012", "Thomas", primaryAdvisor);

        ProductType type = productTypeRepository.save(ProductType.builder().name("Commodities").build());
        Provider provider = providerRepository.save(Provider.builder().name("BlackRock").build());

        productRepository.save(Product.builder()
                .name("Gold ETF")
                .amount(new BigDecimal("10000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(type)
                .provider(provider)
                .build());

        testClient.setAdvisor(secondaryAdvisor);
        clientRepository.save(testClient);
        clientRepository.flush();

        productRepository.save(Product.builder()
                .name("Silver ETF")
                .amount(new BigDecimal("5000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(type)
                .provider(provider)
                .managedBy(secondaryAdvisor)
                .build());


        testClient.setAdvisor(primaryAdvisor);
        clientRepository.save(testClient);
        clientRepository.flush();

        ClientProductsDTO result = productService.getClientProducts(testClient.getClientUid(), secondaryAdvisor.getEmployeeId());

        // Secondary advisor should ONLY see the Silver ETF they manage
        assertThat(result.products()).hasSize(1);
        assertThat(result.products().getFirst().name()).isEqualTo("Silver ETF");
    }

    @Test
    void getClientProducts_ThrowsResourceNotFoundException_WhenRogueAdvisor() {
        Advisor primaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                114L, "EMP-0114", "10000014", "Primary");
        Advisor rogueAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                115L, "EMP-0115", "10000015", "Rogue");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                114L, "CLI-0114", "1000000014", "100000014", "Jackson", primaryAdvisor);

        assertThatThrownBy(() -> productService.getClientProducts(testClient.getClientUid(), rogueAdvisor.getEmployeeId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client not found or access denied");
    }

    // --- SEARCH TESTS ---

    @Test
    void searchProducts_ReturnsFilteredResults_BasedOnCriteria() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                116L, "EMP-0116", "10000016", "SearchingAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                116L, "CLI-0116", "1000000016", "100000016", "White", testAdvisor);

        ProductType type = productTypeRepository.save(ProductType.builder().name("Real Estate").build());
        Provider targetProvider = providerRepository.save(Provider.builder().name("REIT Corp").build());
        Provider otherProvider = providerRepository.save(Provider.builder().name("Other Corp").build());

        productRepository.save(Product.builder()
                .name("Target Property Fund")
                .amount(new BigDecimal("50000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(type)
                .provider(targetProvider)
                .build());

        productRepository.save(Product.builder()
                .name("Other Fund")
                .amount(new BigDecimal("10000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(type)
                .provider(otherProvider)
                .build());

        ProductSearchCriteriaDTO criteria = new ProductSearchCriteriaDTO(
                List.of(targetProvider.getId()),
                null,
                false,
                null,
                null
        );

        Page<ProductDTO> result = productService.searchProducts(criteria, testAdvisor.getEmployeeId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Target Property Fund");
    }
}