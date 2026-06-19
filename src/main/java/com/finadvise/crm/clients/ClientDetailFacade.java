package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressMapper;
import com.finadvise.crm.assets.AssetService;
import com.finadvise.crm.assets.ClientAssetsDTO;
import com.finadvise.crm.budget.BudgetFullDTO;
import com.finadvise.crm.budget.BudgetService;
import com.finadvise.crm.common.OwnershipValidator;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.documents.ClientDocumentsDTO;
import com.finadvise.crm.documents.DocumentService;
import com.finadvise.crm.products.ClientProductsDTO;
import com.finadvise.crm.products.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientDetailFacade {

    private final ClientService clientService;
    private final AddressMapper addressMapper;
    private final AssetService assetService;
    private final ProductService productService;
    private final DocumentService documentService;
    private final BudgetService budgetService;

    /**
     * Aggregates full client details along with domain summaries.
     * @param clientUid The business identifier of the client.
     * @param employeeId The ID of the authenticated user requesting the data.
     * @return ClientDetailDTO containing all aggregated domain data.
     */
    @Transactional(readOnly = true)
    public ClientDetailDTO getClientDetail(String clientUid, String employeeId) {
        Client coreClient = clientService.getClientEntityByUidSecured(clientUid, employeeId);

        Pageable topFiveAssets = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "value"));
        Pageable topFiveProducts = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "startDate"));
        Pageable topFiveDocuments = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "uploadedAt"));

        ClientAssetsDTO assets = assetService.getClientsAssets(clientUid, employeeId, topFiveAssets);
        ClientProductsDTO products = productService.getClientProducts(clientUid, employeeId, topFiveProducts);
        ClientDocumentsDTO documents = documentService.getClientDocuments(clientUid, employeeId, topFiveDocuments);
        BudgetFullDTO budget = budgetService.getBudget(clientUid, employeeId);

        return new ClientDetailDTO(
                coreClient.getClientUid(),
                coreClient.getPersonalId(),
                coreClient.getBirthDate(),
                coreClient.getFirstName(),
                coreClient.getLastName(),
                coreClient.getOccupation(),
                coreClient.getPhone(),
                coreClient.getEmail(),
                coreClient.getIdCardNumber(),
                coreClient.getIdCardIssueDate(),
                coreClient.getIdCardExpiryDate(),
                coreClient.getIdCardIssuer(),
                addressMapper.toDto(coreClient.getPermanentAddress()),
                addressMapper.toDto(coreClient.getContactAddress()),
                coreClient.getLastUpdate(),
                coreClient.getVersion(),
                assets,
                products,
                documents,
                budget
        );
    }
}