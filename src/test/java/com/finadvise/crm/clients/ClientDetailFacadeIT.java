package com.finadvise.crm.clients;

import com.finadvise.crm.assets.AssetService;
import com.finadvise.crm.assets.ClientAssetsDTO;
import com.finadvise.crm.budget.BudgetFullDTO;
import com.finadvise.crm.budget.BudgetService;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.documents.ClientDocumentsDTO;
import com.finadvise.crm.documents.DocumentService;
import com.finadvise.crm.products.ClientProductsDTO;
import com.finadvise.crm.products.ProductService;
import com.finadvise.crm.users.Advisor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ClientDetailFacadeIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private ClientDetailFacade clientDetailFacade;
    @Autowired private TestFixtureFactory testFixtureFactory;

    @MockitoBean private AssetService assetService;
    @MockitoBean private ProductService productService;
    @MockitoBean private DocumentService documentService;
    @MockitoBean private BudgetService budgetService;

    @Test
    void getClientDetail_AggregatesDataSuccessfully_WhenClientExistsAndIsOwned() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                301L, "EMP-0301", "30000001", "FacadeAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                301L, "CLI-0301", "8001019876", "987654321", "FacadeClient", testAdvisor);

        ClientAssetsDTO mockAssets = mock(ClientAssetsDTO.class);
        ClientProductsDTO mockProducts = mock(ClientProductsDTO.class);
        ClientDocumentsDTO mockDocuments = mock(ClientDocumentsDTO.class);
        BudgetFullDTO mockBudget = mock(BudgetFullDTO.class);

        when(assetService.getClientsAssets(eq(testClient.getClientUid()), eq(testAdvisor.getEmployeeId()), any(Pageable.class)))
                .thenReturn(mockAssets);
        when(productService.getClientProducts(eq(testClient.getClientUid()), eq(testAdvisor.getEmployeeId()), any(Pageable.class)))
                .thenReturn(mockProducts);
        when(documentService.getClientDocuments(eq(testClient.getClientUid()), eq(testAdvisor.getEmployeeId()), any(Pageable.class)))
                .thenReturn(mockDocuments);
        when(budgetService.getBudget(eq(testClient.getClientUid()), eq(testAdvisor.getEmployeeId())))
                .thenReturn(mockBudget);

        ClientDetailDTO result = clientDetailFacade.getClientDetail(testClient.getClientUid(), testAdvisor.getEmployeeId());

        assertThat(result).isNotNull();
        assertThat(result.clientUid()).isEqualTo(testClient.getClientUid());
        assertThat(result.personalId()).isEqualTo(testClient.getPersonalId());
        assertThat(result.lastName()).isEqualTo("FacadeClient");
        assertThat(result.permanentAddress()).isNotNull();
        assertThat(result.contactAddress()).isNotNull();

        assertThat(result.assetsSummary()).isEqualTo(mockAssets);
        assertThat(result.productsSummary()).isEqualTo(mockProducts);
        assertThat(result.documentsSummary()).isEqualTo(mockDocuments);
        assertThat(result.fullBudget()).isEqualTo(mockBudget);

        // Explicitly instantiate the captor locally to guarantee initialization
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(assetService).getClientsAssets(eq(testClient.getClientUid()), eq(testAdvisor.getEmployeeId()), pageableCaptor.capture());
        Pageable assetPageable = pageableCaptor.getValue();
        assertThat(assetPageable.getPageSize()).isEqualTo(5);
        assertThat(assetPageable.getSort().getOrderFor("value")).isNotNull();
        assertThat(assetPageable.getSort().getOrderFor("value").getDirection()).isEqualTo(Sort.Direction.DESC);

        verify(productService).getClientProducts(eq(testClient.getClientUid()), eq(testAdvisor.getEmployeeId()), pageableCaptor.capture());
        Pageable productPageable = pageableCaptor.getValue();
        assertThat(productPageable.getPageSize()).isEqualTo(5);
        assertThat(productPageable.getSort().getOrderFor("startDate")).isNotNull();
        assertThat(productPageable.getSort().getOrderFor("startDate").getDirection()).isEqualTo(Sort.Direction.DESC);

        verify(documentService).getClientDocuments(eq(testClient.getClientUid()), eq(testAdvisor.getEmployeeId()), pageableCaptor.capture());
        Pageable documentPageable = pageableCaptor.getValue();
        assertThat(documentPageable.getPageSize()).isEqualTo(5);
        assertThat(documentPageable.getSort().getOrderFor("uploadedAt")).isNotNull();
        assertThat(documentPageable.getSort().getOrderFor("uploadedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getClientDetail_ThrowsResourceNotFoundException_WhenClientNotOwned() {
        Advisor primaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                302L, "EMP-0302", "30000002", "PrimaryAdvisor");
        Advisor rogueAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                303L, "EMP-0303", "30000003", "RogueAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                302L, "CLI-0302", "8001015678", "567891234", "SecureClient", primaryAdvisor);

        assertThatThrownBy(() -> clientDetailFacade.getClientDetail(testClient.getClientUid(), rogueAdvisor.getEmployeeId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client not found or access denied");

        verifyNoInteractions(assetService, productService, documentService, budgetService);
    }
}
