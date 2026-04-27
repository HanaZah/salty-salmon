package com.finadvise.crm.common;

import com.finadvise.crm.assets.AssetRepository;
import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.products.ProductRepository;
import com.finadvise.crm.users.AdminRepository;
import com.finadvise.crm.users.AdvisorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnershipValidator {

    private final ClientRepository clientRepository;
    private final AssetRepository assetRepository;
    private final ProductRepository productRepository;
    private final AdvisorRepository advisorRepository;
    private final AdminRepository adminRepository;

    public boolean canAccessClient(String clientUid, String employeeId) {
        return clientRepository.existsByClientUidAndAdvisorEmployeeId(clientUid, employeeId);
    }

    public boolean ownsAsset(String clientUid, Long assetId) {
        return assetRepository.existsByIdAndClientClientUid(assetId, clientUid);
    }

    public boolean canModifyProduct(String clientUid, Long productId, String employeeId) {
        return productRepository.canModifyProduct(productId, clientUid, employeeId);
    }

    public boolean hasAnyReadAccessToClientProducts(String clientUid, String employeeId) {
        if (canAccessClient(clientUid, employeeId)) {
            return true;
        }

        return productRepository.existsByClientClientUidAndManagedByEmployeeId(clientUid, employeeId);
    }

    public boolean canAccessUser(String employeeId, String requesterId) {
        return employeeId.equals(requesterId)
                || adminRepository.existsByEmployeeId(requesterId)
                || advisorRepository.existsByEmployeeIdAndManagerEmployeeId(employeeId, requesterId);
    }
}
