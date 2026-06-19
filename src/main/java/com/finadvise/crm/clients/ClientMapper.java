package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientMapper {

    private final AddressMapper addressMapper;

    public ClientDetailDTO toDetailDto(Client client) {
        if (client == null) {
            return null;
        }

        return new ClientDetailDTO(
                client.getClientUid(),
                client.getPersonalId(),
                client.getBirthDate(),
                client.getFirstName(),
                client.getLastName(),
                client.getOccupation(),
                client.getPhone(),
                client.getEmail(),
                client.getIdCardNumber(),
                client.getIdCardIssueDate(),
                client.getIdCardExpiryDate(),
                client.getIdCardIssuer(),
                addressMapper.toDto(client.getPermanentAddress()),
                addressMapper.toDto(client.getContactAddress()),
                client.getLastUpdate(),
                client.getVersion(),
                null, // assetsSummary
                null, // productsSummary
                null, // documentsSummary
                null  // fullBudget
        );
    }

    public ClientSearchMinimalDTO toSearchMinimalDto(ClientSearchMinimal entity) {
        if (entity == null) {
            return null;
        }

        return new ClientSearchMinimalDTO(
                entity.getClientUid(),
                entity.getAdvisorEmployeeId(),
                entity.getPersonalId(),
                entity.getFullName(),
                entity.getBirthDate(),
                entity.getNextBirthday(),
                entity.getIdCardExpiryDate(),
                entity.getLastUpdate(),
                entity.getContactCityName(),
                entity.getContactPsc()
        );
    }
}
