package com.finadvise.crm.common;

import com.finadvise.crm.addresses.*;
import com.finadvise.crm.assets.Asset;
import com.finadvise.crm.assets.AssetRepository;
import com.finadvise.crm.assets.AssetType;
import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.users.Advisor;
import com.finadvise.crm.users.AdvisorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TestFixtureFactory {
    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AdvisorRepository advisorRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private StreetRepository streetRepository;

    public Client getOrCreateTestClient(Long id, String uid, String personalId, String idCardNumber, String lastName,
                                        Advisor advisor) {
        City testCity = cityRepository.findByNameAndPsc("Prague", "123 45")
                .orElseGet(() -> cityRepository.save(City.builder().name("Prague").psc("123 45").build()));
        Street testStreet = streetRepository.findByNameAndCityId("Vodičkova", testCity.getId())
                .orElseGet(() -> streetRepository.save(Street.builder().city(testCity).name("Vodičkova").build()));
        Address testAddress = addressRepository.findByHouseNumberAndStreetId("123/45", testStreet.getId())
                .orElseGet(() -> addressRepository.save(Address.builder().street(testStreet).houseNumber("123/45").build()));

        Client client =  clientRepository.findById(id)
                .orElseGet(() -> clientRepository.save(Client.builder()
                        .id(id)
                        .clientUid(uid)
                        .personalId(personalId)
                        .birthDate(LocalDate.now().minusYears(20))
                        .firstName("Budget")
                        .lastName(lastName)
                        .phone("123456789")
                        .email("budget@" + lastName + ".mail")
                        .idCardNumber(idCardNumber)
                        .idCardIssueDate(LocalDate.now().minusYears(2))
                        .idCardExpiryDate(LocalDate.now().plusYears(13))
                        .idCardIssuer("Some Issuer")
                        .lastUpdate(LocalDate.now())
                        .version(0)
                        .isActive(true)
                        .advisor(advisor)
                        .permanentAddress(testAddress)
                        .contactAddress(testAddress)
                        .build())
                );

        if(!client.getClientUid().equals(uid)) {
            throw new IllegalStateException("Client with id " + id + " already exists with different uid. "
            + "Expected: " + uid + ", Actual: " + client.getClientUid());
        }

        return client;
    }

    public Advisor getOrCreateTestAdvisor(Long id, String uid, String ico, String lastName) {
        Advisor advisor = advisorRepository.save(Advisor.builder()
                        .ico(ico)
                        .manager(null)
                        .id(id)
                        .employeeId(uid)
                        .passwordHash("hashed-pass")
                        .firstName("Budget")
                        .lastName(lastName)
                        .phone("123456789")
                        .email("budget@" + lastName + ".mail")
                        .version(0)
                        .isActive(true)
                        .build());

        if(!advisor.getEmployeeId().equals(uid)) {
            throw new IllegalStateException("Advisor with id " + id + " already exists with different uid. "
            + "Expected: " + uid + ", Actual: " + advisor.getEmployeeId());
        }

        return advisor;
    }

    public Asset getOrCreateTestAsset(String name, Client client, AssetType type, Integer value) {
        return assetRepository.save(Asset.builder()
                .name(name)
                .value(value)
                .client(client)
                .assetType(type)
                .build());
    }
}
