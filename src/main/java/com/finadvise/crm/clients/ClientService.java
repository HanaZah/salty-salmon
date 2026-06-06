package com.finadvise.crm.clients;

import com.finadvise.crm.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;

    public Long getClientId(String clientUid) {
        return clientRepository.findIdByClientUid(clientUid).orElseThrow(
                () -> new ResourceNotFoundException("Client not found with UID: " + clientUid)
        );
    }
}
