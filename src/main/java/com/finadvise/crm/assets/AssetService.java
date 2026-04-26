package com.finadvise.crm.assets;

import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.common.OwnershipValidator;
import com.finadvise.crm.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository assetRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final AssetMapper assetMapper;
    private final OwnershipValidator ownershipValidator;
    private final ClientRepository clientRepository;

    @Transactional
    public ClientAssetsDTO getClientsAssets(String clientUid, String employeeId) {
        Long clientId = clientRepository.findIdByClientUid(clientUid)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if(!ownershipValidator.canAccessClient(clientUid, employeeId)) {
            throw new AccessDeniedException("Assigned advisor mismatch for client assets access");
        }

        List<AssetDTO> assets = assetRepository.findAllByClientId(clientId).stream().map(assetMapper::toDto).toList();
        Long totalValue = assets.stream().mapToLong(AssetDTO::value).sum();

        return new ClientAssetsDTO(clientUid, assets, totalValue);
    }

    @Transactional
    public AssetDTO createAsset(String clientUid, AssetDTO payload, String employeeId) {
        Long clientId = clientRepository.findIdByClientUid(clientUid)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if(!ownershipValidator.canAccessClient(clientUid, employeeId)) {
            throw new AccessDeniedException("Assigned advisor mismatch for client assets access");
        }

        AssetType assetType = assetTypeRepository.findById(payload.assetTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset Type not found with ID: " + payload.assetTypeId()));

        Asset newAsset = Asset.builder()
                .name(payload.name())
                .value(payload.value())
                .note(payload.note())
                .assetType(assetType)
                .client(clientRepository.getReferenceById(clientId))
                .build();

        Asset savedAsset = assetRepository.save(newAsset);
        return assetMapper.toDto(savedAsset);
    }

    @Transactional
    public AssetDTO updateAsset(String clientUid, Long assetId, AssetDTO payload, String advisorEmployeeId) {
        Asset existingAsset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        if (!ownershipValidator.canAccessClient(clientUid, advisorEmployeeId)) {
            throw new AccessDeniedException("Assigned advisor mismatch for client access");
        }

        if (!ownershipValidator.ownsAsset(clientUid, assetId)) {
            throw new AccessDeniedException("Asset does not belong to this client");
        }

        AssetType assetType = assetTypeRepository.findById(payload.assetTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset Type not found with ID: " + payload.assetTypeId()));

        existingAsset.setName(payload.name());
        existingAsset.setValue(payload.value());
        existingAsset.setNote(payload.note());
        existingAsset.setAssetType(assetType);

        Asset updatedAsset = assetRepository.save(existingAsset);
        return assetMapper.toDto(updatedAsset);
    }

    @Transactional
    public void deleteAsset(String clientUid, Long assetId, String advisorEmployeeId) {
        if (!clientRepository.existsByClientUid(clientUid)) {
            throw new ResourceNotFoundException("Client not found");
        }
        if (!ownershipValidator.canAccessClient(clientUid, advisorEmployeeId)) {
            throw new AccessDeniedException("Assigned advisor mismatch for client access");
        }
        if (!ownershipValidator.ownsAsset(clientUid, assetId)) {
            throw new AccessDeniedException("Asset not found or does not belong to this client");
        }

        assetRepository.deleteById(assetId);
    }
}
