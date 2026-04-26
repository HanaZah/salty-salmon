package com.finadvise.crm.common;

import com.finadvise.crm.assets.AssetRepository;
import com.finadvise.crm.clients.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnershipValidator {

    private final ClientRepository clientRepository;
    private final AssetRepository assetRepository;

    public boolean canAccessClient(String clientUid, String employeeId) {
        return clientRepository.existsByClientUidAndAdvisorEmployeeId(clientUid, employeeId);
    }

    public boolean ownsAsset(String clientUid, Long assetId) {
        return assetRepository.existsByIdAndClientClientUid(assetId, clientUid);
    }
}
