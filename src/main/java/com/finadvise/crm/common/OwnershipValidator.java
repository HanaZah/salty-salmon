package com.finadvise.crm.common;

import com.finadvise.crm.clients.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnershipValidator {

    private final ClientRepository clientRepository;

    public boolean canAccessClient(String clientUid, String employeeId) {
        return clientRepository.existsByClientUidAndAdvisorEmployeeId(clientUid, employeeId);
    }
}
