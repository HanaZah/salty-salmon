package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.*;
import com.finadvise.crm.common.*;
import com.finadvise.crm.users.Advisor;
import com.finadvise.crm.users.AdvisorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;
    private final OwnershipValidator ownershipValidator;
    private final AddressService addressService;
    private final AddressRepository addressRepository;
    private final AdvisorRepository advisorRepository;
    private final ObfuscatedIdGenerator uidGenerator;
    private final ClientMapper clientMapper;
    private final BirthdayProcessor birthdayProcessor;
    private final ClientSearchMinimalRepository searchMinimalRepository;
    private final Clock clock;

    public Long getClientId(String clientUid) {
        return clientRepository.findIdByClientUid(clientUid).orElseThrow(
                () -> new ResourceNotFoundException("Client not found with UID: " + clientUid)
        );
    }

    @Transactional(readOnly = true)
    public Page<ClientDashboardSummary> getRecentClientSummaries(String employeeId, boolean isAdmin, int pageSize) {
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "lastUpdate"));

        if (isAdmin) {
            return clientRepository.findAllClientSummaries(pageable);
        } else {
            return clientRepository.findClientSummariesByEmployeeId(employeeId, pageable);
        }
    }

    @Transactional(readOnly = true)
    public Client getClientEntityByUidSecured(String clientUid, String employeeId) {
        if (!ownershipValidator.canAccessClient(clientUid, employeeId)) {
            throw new ResourceNotFoundException("Client not found or access denied");
        }

        return clientRepository.findByClientUidAndIsActiveTrue(clientUid)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found or access denied"));
    }

    @Transactional
    public void updateClientDetails(String clientUid, ClientUpdateDetailsRequestDTO request, String employeeId) {
        Client client = getClientEntityByUidSecured(clientUid, employeeId);

        verifyOptimisticLock(client, request.version());

        client.setFirstName(request.firstName());
        client.setLastName(request.lastName());
        client.setOccupation(request.occupation());
        client.setPhone(request.phone());
        client.setEmail(request.email());

        updateAddressIfChanged(client, request.permanentAddress(), true);
        updateAddressIfChanged(client, request.contactAddress(), false);
    }

    private void updateAddressIfChanged(Client client, AddressInputDTO incomingAddress, boolean isPermanent) {
        boolean hasChanged = hasChanged(client, incomingAddress, isPermanent);

        if (hasChanged) {
            AddressDTO resolvedAddress = addressService.findOrCreateAddress(incomingAddress);
            Address newAddressRef = addressRepository.getReferenceById(resolvedAddress.id());

            if (isPermanent) {
                client.setPermanentAddress(newAddressRef);
            } else {
                client.setContactAddress(newAddressRef);
            }
        }
    }

    private static boolean hasChanged(Client client, AddressInputDTO incomingAddress, boolean isPermanent) {
        Address currentAddress = isPermanent ? client.getPermanentAddress() : client.getContactAddress();

        return currentAddress == null || !currentAddress.matches(
                incomingAddress.street(),
                incomingAddress.houseNumber(),
                incomingAddress.city(),
                incomingAddress.postalCode()
        );
    }

    @Transactional
    public void updateClientIdCard(String clientUid, ClientUpdateIdCardRequestDTO request, String employeeId) {
        Client client = getClientEntityByUidSecured(clientUid, employeeId);

        verifyOptimisticLock(client, request.version());

        if (request.idCardIssueDate().isAfter(request.idCardExpiryDate())) {
            throw new InvalidInputValueException("ID card issue date must precede the expiry date.");
        }

        client.setIdCardNumber(request.idCardNumber());
        client.setIdCardIssueDate(request.idCardIssueDate());
        client.setIdCardExpiryDate(request.idCardExpiryDate());
        client.setIdCardIssuer(request.idCardIssuer());

    }

    @Transactional
    public ClientDetailDTO createClient(ClientCreateRequestDTO request, String requesterEmployeeId, boolean isAdmin) {
        if (clientRepository.existsByPersonalId(request.personalId())) {
            throw new ResourceConflictException("Client with this personal ID already exists.");
        }

        if (request.idCardIssueDate().isAfter(request.idCardExpiryDate())) {
            throw new InvalidInputValueException("ID card issue date must precede the expiry date.");
        }

        String targetAdvisorId = requesterEmployeeId;
        if (isAdmin) {
            if (request.advisorEmployeeId() == null || request.advisorEmployeeId().isBlank()) {
                throw new InvalidInputValueException("Administrators must explicitly provide an advisorEmployeeId when creating a client.");
            }
            targetAdvisorId = request.advisorEmployeeId();
        }

        Advisor advisor = advisorRepository.findByEmployeeId(targetAdvisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Target Advisor not found."));

        Address permanentRef = resolveAddressEntity(request.permanentAddress());
        Address contactRef = resolveAddressEntity(request.contactAddress());
        LocalDate nextBirthday = birthdayProcessor.calculateNextBirthday(request.birthDate());
        Long nextId = clientRepository.getNextSequenceValue();
        String clientUid = uidGenerator.encode(nextId);

        Client newClient = Client.builder()
                .id(nextId)
                .clientUid(clientUid)
                .personalId(request.personalId())
                .birthDate(request.birthDate())
                .nextBirthday(nextBirthday)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .occupation(request.occupation())
                .phone(request.phone())
                .email(request.email())
                .idCardNumber(request.idCardNumber())
                .idCardIssueDate(request.idCardIssueDate())
                .idCardExpiryDate(request.idCardExpiryDate())
                .idCardIssuer(request.idCardIssuer())
                .advisor(advisor)
                .permanentAddress(permanentRef)
                .contactAddress(contactRef)
                .build();

        return clientMapper.toDetailDto(clientRepository.save(newClient));
    }

    @Transactional
    public void deleteClient(String clientUid, String employeeId, boolean isAdmin) {
        Client client = clientRepository.findByClientUidAndIsActiveTrue(clientUid)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found or access denied"));

        if (!isAdmin && !ownershipValidator.canAccessClient(clientUid, employeeId)) {
            throw new ResourceNotFoundException("Client not found or access denied");
        }

        client.setActive(false);
    }

    private Address resolveAddressEntity(AddressInputDTO dto) {
        AddressDTO resolvedAddress = addressService.findOrCreateAddress(dto);
        // Using getReferenceById prevents an unnecessary database SELECT query
        return addressRepository.getReferenceById(resolvedAddress.id());
    }

    @Transactional(readOnly = true)
    public Page<ClientSearchMinimalDTO> searchClients(
            ClientSearchCriteriaDTO criteria,
            String requesterEmployeeId,
            boolean isAdmin,
            Pageable pageable) {

        ClientSearchCriteriaDTO secureCriteria = criteria;
        if (!isAdmin) {
            // Prevent ID enumeration and data leakage by overriding the parameter
            secureCriteria = criteria.withAdvisorEmployeeId(requesterEmployeeId);
        }

        LocalDate today = LocalDate.now(clock);
        Specification<ClientSearchMinimal> spec =
                ClientSpecifications.withCriteria(secureCriteria, today, true);

        Page<ClientSearchMinimal> results = searchMinimalRepository.findAll(spec, pageable);

        return results.map(clientMapper::toSearchMinimalDto);
    }

    private void verifyOptimisticLock(Client client, Integer requestedVersion) {
        if (requestedVersion == null) {
            throw new MissingVersionException("Version must be provided for updates.");
        }
        if (!client.getVersion().equals(requestedVersion)) {
            throw new ObjectOptimisticLockingFailureException(Client.class, Objects.requireNonNull(client.getId()));
        }
    }
}
