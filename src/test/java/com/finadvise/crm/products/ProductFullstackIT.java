package com.finadvise.crm.products;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.documents.Document;
import com.finadvise.crm.documents.DocumentRepository;
import com.finadvise.crm.documents.DocumentType;
import com.finadvise.crm.documents.DocumentTypeRepository;
import com.finadvise.crm.users.Advisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ProductFullstackIT {

    @Container
    @ServiceConnection
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductTypeRepository productTypeRepository;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentTypeRepository documentTypeRepository;

    private Advisor testAdvisor;
    private Client testClient;
    private ProductType testProductType;
    private Provider testProvider;

    @BeforeEach
    void setUp() {
        testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                201L, "ADV-0001", "11111111", "PrimaryOwner");

        testClient = testFixtureFactory.getOrCreateTestClient(
                201L, "CLI-0001", "1234567890", "123456789", "Smith", testAdvisor);

        testProductType = productTypeRepository.findByName("Life Insurance")
                .orElseGet(() -> productTypeRepository.save(ProductType.builder().name("Life Insurance").build()));

        testProvider = providerRepository.findByName("Allianz")
                .orElseGet(() -> providerRepository.save(Provider.builder().name("Allianz").build()));
    }

    // --- GET CLIENT PRODUCTS TESTS ---

    @Test
    @WithMockUser(username = "ADV-0001", authorities = "ADVISOR")
    void getClientProducts_Success_Returns200AndProductList() throws Exception {
        productRepository.save(Product.builder()
                .name("Secure Future Plan")
                .amount(new BigDecimal("5000.00"))
                .startDate(LocalDate.now())
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(testProductType)
                .provider(testProvider)
                .build());

        // Added page and size params, updated JSON paths to target the 'content' array
        mockMvc.perform(get("/api/v1/clients/{clientUid}/products", testClient.getClientUid())
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientUid").value(testClient.getClientUid()))
                .andExpect(jsonPath("$.totalActive").value(1))
                .andExpect(jsonPath("$.products.content", hasSize(1)))
                .andExpect(jsonPath("$.products.content[0].name").value("Secure Future Plan"));
    }

    @Test
    @WithMockUser(username = "ROG-0002", authorities = "ADVISOR")
    void getClientProducts_Fails_ReturnsOpaque404_WhenNotAssignedAdvisor() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientUid}/products", testClient.getClientUid())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Client not found or access denied"));
    }

    // --- CREATE PRODUCT TESTS ---

    @Test
    @WithMockUser(username = "ADV-0001", authorities = "ADVISOR")
    void createProduct_Success_Returns201AndCreatedDto() throws Exception {
        ProductDTO payload = new ProductDTO(
                null, "Premium Life Plan", new BigDecimal("2500.00"),
                LocalDate.now(), null, testProductType.getId(), null, testProvider.getId(), null, null
        );

        mockMvc.perform(post("/api/v1/clients/{clientUid}/products", testClient.getClientUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Premium Life Plan"))
                .andExpect(jsonPath("$.productTypeName").value("Life Insurance"))
                .andExpect(jsonPath("$.providerName").value("Allianz"));

        Page<Product> savedProductsPage = productRepository.findAllByClientClientUid(
                testClient.getClientUid(), PageRequest.of(0, 10));
        assertThat(savedProductsPage.getContent()).hasSize(1);
    }

    @Test
    @WithMockUser(username = "ADV-0001", authorities = "ADVISOR")
    void createProduct_Fails_Returns400_WhenValidationFails() throws Exception {
        // Amount is negative, violating @DecimalMin("0.00")
        ProductDTO invalidPayload = new ProductDTO(
                null, "", new BigDecimal("-100.00"),
                LocalDate.now(), null, testProductType.getId(), null, testProvider.getId(), null, null
        );

        mockMvc.perform(post("/api/v1/clients/{clientUid}/products", testClient.getClientUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @WithMockUser(username = "ROG-0002", authorities = "ADVISOR")
    void createProduct_Fails_Returns403_WhenAdvisorDoesNotOwnClient() throws Exception {
        // Service layer directly throws AccessDeniedException on creation
        ProductDTO payload = new ProductDTO(
                null, "Rogue Plan", new BigDecimal("100.00"),
                LocalDate.now(), null, testProductType.getId(), null, testProvider.getId(), null, null
        );

        mockMvc.perform(post("/api/v1/clients/{clientUid}/products", testClient.getClientUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access Denied"))
                .andExpect(jsonPath("$.detail").value("Client not found or access denied"));
    }

    // --- UPDATE PRODUCT TESTS ---

    @Test
    @WithMockUser(username = "ADV-0001", authorities = "ADVISOR")
    void updateProduct_Success_Returns200AndUpdatedDto() throws Exception {
        Product existingProduct = productRepository.save(Product.builder()
                .name("Legacy Fund")
                .amount(new BigDecimal("10000.00"))
                .startDate(LocalDate.now().minusYears(1))
                .nextAnniversary(LocalDate.now().plusMonths(1))
                .client(testClient)
                .productType(testProductType)
                .provider(testProvider)
                .build());

        ProductDTO updatePayload = new ProductDTO(
                existingProduct.getId(), "Modernized Fund", new BigDecimal("15000.00"),
                LocalDate.now(), LocalDate.now().plusYears(10), testProductType.getId(), null,
                testProvider.getId(), null, null
        );

        mockMvc.perform(put("/api/v1/clients/{clientUid}/products/{productId}",
                        testClient.getClientUid(), existingProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Modernized Fund"))
                .andExpect(jsonPath("$.amount").value(15000.00));
    }

    @Test
    @WithMockUser(username = "ROG-0002", authorities = "ADVISOR")
    void updateProduct_ReturnsOpaque404_ToPreventIdEnumeration() throws Exception {
        Product existingProduct = productRepository.save(Product.builder()
                .name("Top Secret Fund")
                .amount(new BigDecimal("99999.00"))
                .startDate(LocalDate.now())
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(testProductType)
                .provider(testProvider)
                .build());

        ProductDTO updatePayload = new ProductDTO(
                existingProduct.getId(), "Hacked Fund", new BigDecimal("0.00"),
                LocalDate.now(), null, testProductType.getId(), null, testProvider.getId(), null, null
        );

        mockMvc.perform(put("/api/v1/clients/{clientUid}/products/{productId}",
                        testClient.getClientUid(), existingProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    // --- DELETE PRODUCT TESTS ---

    @Test
    @WithMockUser(username = "ADMIN-0403", authorities = "ADMIN")
    void deleteProduct_Success_Returns204_WhenUnlinkedProduct() throws Exception {
        Product testProduct = Product.builder()
                .name("TestProduct")
                .amount(new BigDecimal("10000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .productType(testProductType)
                .client(testClient)
                .provider(testProvider)
                .managedBy(testAdvisor)
                .build();

        productRepository.save(testProduct);
        documentRepository.deleteAll();

        mockMvc.perform(delete("/api/v1/clients/{clientUid}/products/{productId}",
                        testClient.getClientUid(), testProduct.getId()))
                .andExpect(status().isNoContent());

        assertThat(productRepository.existsById(testProduct.getId())).isFalse();
    }

    @Test
    @WithMockUser(username = "ADMIN-0403", authorities = "ADMIN")
    void deleteProduct_Fails_Returns404_WhenProductDoesNotBelongToClient() throws Exception {
        String wrongClientUid = "OTHER-UID";
        Product testProduct = Product.builder()
                .name("TestProduct")
                .amount(new BigDecimal("10000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .productType(testProductType)
                .client(testClient)
                .provider(testProvider)
                .managedBy(testAdvisor)
                .build();

        productRepository.save(testProduct);

        mockMvc.perform(delete("/api/v1/clients/{clientUid}/products/{productId}",
                        wrongClientUid, testProduct.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.type").value("https://api.finadvise.com/errors/not-found"));
    }

    @Test
    @WithMockUser(username = "ADMIN-0403", authorities = "ADMIN")
    void deleteProduct_Fails_Returns409_WhenDocumentsLinked() throws Exception {
        Product testProduct = Product.builder()
                .name("TestProduct")
                .amount(new BigDecimal("10000.00"))
                .startDate(LocalDate.now().minusDays(10))
                .nextAnniversary(LocalDate.now().plusYears(1))
                .productType(testProductType)
                .client(testClient)
                .provider(testProvider)
                .managedBy(testAdvisor)
                .build();

        productRepository.save(testProduct);

        DocumentType testDocumentType = documentTypeRepository.save(DocumentType.builder().name("TestDocType").build());
        Document document = Document.builder()
                .fileName("policy.pdf")
                .filePath("s3://bucket/policy.pdf")
                .documentType(testDocumentType)
                .client(testClient)
                .product(testProduct)
                .isActive(true)
                .build();
        documentRepository.save(document);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/clients/{clientUid}/products/{productId}",
                        testClient.getClientUid(), testProduct.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail").value("Cannot delete product: Linked documents exist. All documents must be hard-deleted first."))
                .andExpect(jsonPath("$.type").value("https://api.finadvise.com/errors/resource-conflict"));
    }

    // --- SEARCH PRODUCTS TESTS ---

    @Test
    @WithMockUser(username = "ADV-0001", authorities = "ADVISOR")
    void searchProducts_Success_ReturnsPaginatedResults() throws Exception {
        productRepository.save(Product.builder()
                .name("Indexed Fund")
                .amount(new BigDecimal("1000.00"))
                .startDate(LocalDate.now())
                .nextAnniversary(LocalDate.now().plusYears(1))
                .client(testClient)
                .productType(testProductType)
                .provider(testProvider)
                .build());

        ProductSearchCriteriaDTO criteria = new ProductSearchCriteriaDTO(
                List.of(testProvider.getId()), null, false, null, null
        );

        mockMvc.perform(post("/api/v1/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criteria))
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Indexed Fund"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
