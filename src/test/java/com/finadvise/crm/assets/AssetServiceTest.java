package com.finadvise.crm.assets;

import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.common.OwnershipValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
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
    void getClientAssets_ReturnsZeroTotal_WhenListIsEmpty() {
        String clientUid = "UID123";
        String advisorId = "ADV_01";
        Long clientId = 99L;
        Pageable pageable = PageRequest.of(0, 10);

        when(ownershipValidator.canAccessClient(clientUid, advisorId)).thenReturn(true);
        when(clientRepository.findIdByClientUid(clientUid)).thenReturn(Optional.of(clientId));

        when(assetRepository.findAllByClientId(clientId, pageable)).thenReturn(new PageImpl<>(Collections.emptyList()));
        // The DB COALESCE handles returning 0L for empty sets
        when(assetRepository.sumValueByClientId(clientId)).thenReturn(0L);

        ClientAssetsDTO result = assetService.getClientsAssets(clientUid, advisorId, pageable);

        assertThat(result.totalValue()).isEqualTo(0L);
        assertThat(result.assets().getContent()).isEmpty();
    }
}