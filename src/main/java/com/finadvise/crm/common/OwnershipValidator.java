package com.finadvise.crm.common;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnershipValidator {

    private final ClientRepository clientRepository;

    public boolean canAccessClient(Long clientId, String employeeId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        return client.getAdvisor().getEmployeeId().equals(employeeId);
    }
}
