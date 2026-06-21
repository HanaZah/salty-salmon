package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressInputDTO;
import com.finadvise.crm.common.InvalidInputValueException;
import com.finadvise.crm.common.ResourceConflictException;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.users.Advisor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ClientServiceIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private ClientService clientService;
    @Autowired private ClientRepository clientRepository;
    @Autowired private TestFixtureFactory testFixtureFactory;

    private static final LocalDate VALID_BIRTH_DATE = LocalDate.now().minusYears(30);

    @Test
    void createClient_SavesSuccessfully_WithValidData() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                201L, "EMP-0201", "20000001", "CreateAdvisor");

        AddressInputDTO validAddress = new AddressInputDTO("Test Street", "123", "Prague", "110 00");
        ClientCreateRequestDTO payload = new ClientCreateRequestDTO(
                "8001011234", VALID_BIRTH_DATE, "John", "Doe", "Developer", "+420123456789", "john@test.com",
                "123456789", LocalDate.now().minusDays(10), LocalDate.now().plusYears(10), "Ministry",
                validAddress, validAddress, null
        );

        ClientDetailDTO result = clientService.createClient(payload, testAdvisor.getEmployeeId(), false);

        assertThat(result.clientUid()).isNotNull();
        assertThat(result.personalId()).isEqualTo("8001011234");
        assertThat(result.firstName()).isEqualTo("John");

        Client savedClient = clientRepository.findByClientUidAndIsActiveTrue(result.clientUid()).orElseThrow();
        assertThat(savedClient.getAdvisor().getId()).isEqualTo(testAdvisor.getId());
        assertThat(savedClient.getPermanentAddress()).isNotNull();
    }

    @Test
    void createClient_ThrowsResourceConflictException_WhenPersonalIdExists() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                202L, "EMP-0202", "20000002", "ConflictAdvisor");
        testFixtureFactory.getOrCreateTestClient(
                202L, "CLI-0202", "8001019999", "999999999", "Existing", testAdvisor);

        AddressInputDTO validAddress = new AddressInputDTO("Test Street", "123", "Prague", "110 00");
        ClientCreateRequestDTO payload = new ClientCreateRequestDTO(
                "8001019999", VALID_BIRTH_DATE, "Jane", "Doe", "Teacher", "+420123456789", "jane@test.com",
                "111222333", LocalDate.now().minusDays(10), LocalDate.now().plusYears(10), "Ministry",
                validAddress, validAddress, null
        );

        assertThatThrownBy(() -> clientService.createClient(payload, testAdvisor.getEmployeeId(), false))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Client with this personal ID already exists");
    }

    @Test
    void createClient_ThrowsIllegalArgumentException_WhenIdCardExpiryBeforeIssue() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                203L, "EMP-0203", "20000003", "DateAdvisor");

        AddressInputDTO validAddress = new AddressInputDTO("Test Street", "123", "Prague", "110 00");
        ClientCreateRequestDTO payload = new ClientCreateRequestDTO(
                "8001018888", VALID_BIRTH_DATE, "Jack", "Smith", "Driver", "+420123456789", "jack@test.com",
                "444555666", LocalDate.now(), LocalDate.now().minusDays(1), "Ministry",
                validAddress, validAddress, null
        );

        assertThatThrownBy(() -> clientService.createClient(payload, testAdvisor.getEmployeeId(), false))
                .isInstanceOf(InvalidInputValueException.class)
                .hasMessageContaining("ID card issue date must precede the expiry date");
    }

    @Test
    void updateClientDetails_UpdatesFieldsAndResolvesNewAddress() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                204L, "EMP-0204", "20000004", "UpdateAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                204L, "CLI-0204", "8001017777", "777777777", "OldLastName", testAdvisor);

        Long oldPermAddressId = testClient.getPermanentAddress().getId();

        AddressInputDTO newPermAddress = new AddressInputDTO("New Street", "456", "Brno", "602 00");
        AddressInputDTO oldContactAddress = new AddressInputDTO(
                testClient.getContactAddress().getStreet().getName(),
                testClient.getContactAddress().getHouseNumber(),
                testClient.getContactAddress().getStreet().getCity().getName(),
                testClient.getContactAddress().getStreet().getCity().getPsc()
        );

        ClientUpdateDetailsRequestDTO payload = new ClientUpdateDetailsRequestDTO(
                "UpdatedName", "NewLastName", "NewOccupation", "+420987654321", "new@test.com",
                newPermAddress, oldContactAddress, testClient.getVersion()
        );

        clientService.updateClientDetails(testClient.getClientUid(), payload, testAdvisor.getEmployeeId());
        clientRepository.flush();

        Client updatedClient = clientRepository.findByClientUidAndIsActiveTrue(testClient.getClientUid()).orElseThrow();

        assertThat(updatedClient.getFirstName()).isEqualTo("UpdatedName");
        assertThat(updatedClient.getOccupation()).isEqualTo("NewOccupation");
        assertThat(updatedClient.getPermanentAddress().getId()).isNotEqualTo(oldPermAddressId);
        assertThat(updatedClient.getContactAddress().getId()).isEqualTo(testClient.getContactAddress().getId());
    }

    @Test
    void updateClientIdCard_UpdatesSuccessfully_WhenAuthorized() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                205L, "EMP-0205", "20000005", "IdCardAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                205L, "CLI-0205", "8001016666", "666666666", "CardHolder", testAdvisor);

        ClientUpdateIdCardRequestDTO payload = new ClientUpdateIdCardRequestDTO(
                "123123123", LocalDate.now().minusDays(5), LocalDate.now().plusYears(5),
                "New Issuer", testClient.getVersion()
        );

        clientService.updateClientIdCard(testClient.getClientUid(), payload, testAdvisor.getEmployeeId());
        clientRepository.flush();

        Client updatedClient = clientRepository.findByClientUidAndIsActiveTrue(testClient.getClientUid()).orElseThrow();
        assertThat(updatedClient.getIdCardNumber()).isEqualTo("123123123");
        assertThat(updatedClient.getIdCardIssuer()).isEqualTo("New Issuer");
    }

    @Test
    void deleteClient_SoftDeletes_WhenRequestedByOwner() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                206L, "EMP-0206", "20000006", "DeleteAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                206L, "CLI-0206", "8001015555", "555555555", "ToDelete", testAdvisor);

        clientService.deleteClient(testClient.getClientUid(), testAdvisor.getEmployeeId(), false);
        clientRepository.flush();

        assertThat(clientRepository.findByClientUidAndIsActiveTrue(testClient.getClientUid())).isEmpty();
    }

    @Test
    void deleteClient_SoftDeletes_WhenRequestedByAdmin() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                207L, "EMP-0207", "20000007", "TargetAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                207L, "CLI-0207", "8001014444", "444444444", "TargetClient", testAdvisor);

        clientService.deleteClient(testClient.getClientUid(), "ADMIN-EMP-ID", true);
        clientRepository.flush();

        assertThat(clientRepository.findByClientUidAndIsActiveTrue(testClient.getClientUid())).isEmpty();
    }

    @Test
    void accessSecuredMethods_ThrowsResourceNotFoundException_ForRogueAdvisor() {
        Advisor primaryAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                208L, "EMP-0208", "20000008", "Primary");
        Advisor rogueAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                209L, "EMP-0209", "20000009", "Rogue");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                208L, "CLI-0208", "8001013333", "333333333", "SecureClient", primaryAdvisor);

        assertThatThrownBy(() -> clientService.getClientEntityByUidSecured(testClient.getClientUid(), rogueAdvisor.getEmployeeId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client not found or access denied");

        assertThatThrownBy(() -> clientService.deleteClient(testClient.getClientUid(), rogueAdvisor.getEmployeeId(), false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client not found or access denied");
    }

    @Test
    void searchClients_ReturnsResults_FromDatabaseView() {
        Advisor testAdvisor = testFixtureFactory.getOrCreateTestAdvisor(
                210L, "EMP-0210", "20000010", "SearchAdvisor");
        Client testClient = testFixtureFactory.getOrCreateTestClient(
                210L, "CLI-0210", "8001012222", "222222222", "SearchableClient", testAdvisor);

        testClient.setFirstName("UniqueSearchName");
        clientRepository.saveAndFlush(testClient);

        ClientSearchCriteriaDTO criteria = new ClientSearchCriteriaDTO(
                null, "UniqueSearchName", null, null, null, null, null, null, null, null, null, null
        );

        Page<ClientSearchMinimalDTO> result = clientService.searchClients(
                criteria, testAdvisor.getEmployeeId(), false, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent().stream().anyMatch(c -> c.clientUid().equals(testClient.getClientUid()))).isTrue();
        assertThat(result.getContent().getFirst().advisorEmployeeId()).isEqualTo(testAdvisor.getEmployeeId());
    }
}
