package com.finadvise.crm.assets;

import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.common.OwnershipValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock private AssetRepository assetRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AssetMapper assetMapper;
    @Mock private OwnershipValidator ownershipValidator;

    @InjectMocks
    private AssetService assetService;

    @Test
    void getClientAssets_CalculatesTotalValueCorrectly() {
        String clientUid = "UID123";
        String advisorId = "ADV_01";
        Long clientId = 99L;

        Asset mockAsset1 = Asset.builder().id(1L).value(100).build();
        Asset mockAsset2 = Asset.builder().id(2L).value(300).build();

        AssetDTO dto1 = new AssetDTO(1L, "A", 100, null, 1L, null);
        AssetDTO dto2 = new AssetDTO(2L, "B", 300, null, 1L, null);

        when(ownershipValidator.canAccessClient(clientUid, advisorId)).thenReturn(true);
        when(clientRepository.findIdByClientUid(clientUid)).thenReturn(Optional.of(clientId));
        when(assetRepository.findAllByClientId(clientId)).thenReturn(List.of(mockAsset1, mockAsset2));
        when(assetMapper.toDto(mockAsset1)).thenReturn(dto1);
        when(assetMapper.toDto(mockAsset2)).thenReturn(dto2);

        ClientAssetsDTO result = assetService.getClientsAssets(clientUid, advisorId);

        assertThat(result.totalValue()).isEqualTo(400L); // 100 + 300
        assertThat(result.assets()).hasSize(2);
    }

    @Test
    void getClientAssets_ReturnsZeroTotal_WhenListIsEmpty() {
        String clientUid = "UID123";
        String advisorId = "ADV_01";
        Long clientId = 99L;

        when(ownershipValidator.canAccessClient(clientUid, advisorId)).thenReturn(true);
        when(clientRepository.findIdByClientUid(clientUid)).thenReturn(Optional.of(clientId));
        when(assetRepository.findAllByClientId(clientId)).thenReturn(Collections.emptyList());

        ClientAssetsDTO result = assetService.getClientsAssets(clientUid, advisorId);

        assertThat(result.totalValue()).isEqualTo(0L);
        assertThat(result.assets()).isEmpty();
    }
}