package com.finadvise.crm.assets;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.common.TestFixtureFactory;
import com.finadvise.crm.users.Advisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@ActiveProfiles("test")
public class AssetFullstackIT {

    @Container
    @ServiceConnection
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AssetTypeRepository assetTypeRepository;
    @Autowired private TestFixtureFactory fixtureFactory;

    private AssetType realEstateType;
    private Client testClient;

    @BeforeEach
    void setUp() {
        Advisor advisor = fixtureFactory.getOrCreateTestAdvisor(1234L, "ADV_01", "12312312", "Assets");
        testClient = fixtureFactory.getOrCreateTestClient(
                11L, "A1B2C3D4", "0987654321", "987654321", "Smith", advisor);

        realEstateType = assetTypeRepository.findByName("Nemovitost")
                .orElseGet(() -> assetTypeRepository.save(new AssetType(null, "Nemovitost")));
    }

    @Test
    @WithMockUser(username = "ADV_01", roles = "ADVISOR")
    void getClientAssets_Success_ReturnsListAndTotalValue() throws Exception {
        fixtureFactory.getOrCreateTestAsset("Byt Praha", testClient, realEstateType, 5000000);
        fixtureFactory.getOrCreateTestAsset("Chata", testClient, realEstateType, 1500000);

        mockMvc.perform(get("/api/v1/clients/{clientUid}/assets", testClient.getClientUid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientUid").value(testClient.getClientUid()))
                .andExpect(jsonPath("$.totalValue").value(6500000))
                .andExpect(jsonPath("$.assets").isArray())
                .andExpect(jsonPath("$.assets.length()").value(2));
    }

    @Test
    @WithMockUser(username = "WRONG_ADV", roles = "ADVISOR")
    void getClientAssets_Forbidden_WhenNotAssignedAdvisor() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientUid}/assets", testClient.getClientUid()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ADV_01", roles = "ADVISOR")
    void getClientAssets_Forbidden_WhenClientUidIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientUid}/assets", "INVALID_UID"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ADV_01", roles = "ADVISOR")
    void createAsset_Success_Returns201AndDto() throws Exception {
        AssetDTO payload = new AssetDTO(null, "Garáž", 500000, "V centru", realEstateType.getId(), null);

        mockMvc.perform(post("/api/v1/clients/{clientUid}/assets", testClient.getClientUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Garáž"))
                .andExpect(jsonPath("$.value").value(500000));
    }

    @Test
    @WithMockUser(username = "ADV_01", roles = "ADVISOR")
    void createAsset_BadRequest_WhenValidationFails() throws Exception {
        AssetDTO invalidPayload = new AssetDTO(null, "", -100, null, realEstateType.getId(), null);

        mockMvc.perform(post("/api/v1/clients/{clientUid}/assets", testClient.getClientUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "ADV_01", roles = "ADVISOR")
    void updateAsset_Success_Returns200AndUpdatedDto() throws Exception {
        Asset existingAsset = fixtureFactory.getOrCreateTestAsset("Staré Jméno", testClient, realEstateType, 1000000);

        AssetDTO updatePayload = new AssetDTO(
                existingAsset.getId(), "Nové Jméno", 2000000, "Zrekonstruováno", realEstateType.getId(),
                null
        );

        mockMvc.perform(put("/api/v1/clients/{clientUid}/assets/{assetId}", testClient.getClientUid(), existingAsset.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nové Jméno"))
                .andExpect(jsonPath("$.value").value(2000000))
                .andExpect(jsonPath("$.note").value("Zrekonstruováno"));
    }

    @Test
    @WithMockUser(username = "ADV_01", roles = "ADVISOR")
    void updateAsset_Forbidden_WhenAssetIdDoesNotExist() throws Exception {
        AssetDTO updatePayload = new AssetDTO(
                999L, "Valid Name", 10000, null, realEstateType.getId(), null
        );

        mockMvc.perform(put("/api/v1/clients/{clientUid}/assets/{assetId}", testClient.getClientUid(), 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ADV_01", roles = "ADVISOR")
    void deleteAsset_Success_Returns204() throws Exception {
        Asset existingAsset = fixtureFactory.getOrCreateTestAsset("K smazání", testClient, realEstateType, 100);

        mockMvc.perform(delete(
                "/api/v1/clients/{clientUid}/assets/{assetId}", testClient.getClientUid(), existingAsset.getId()
                ))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/clients/{clientUid}/assets", testClient.getClientUid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets.length()").value(0));
    }

    @Test
    @WithMockUser(username = "WRONG_ADV", roles = "ADVISOR")
    void deleteAsset_Forbidden_WhenNotAssignedAdvisor() throws Exception {
        Asset existingAsset = fixtureFactory.getOrCreateTestAsset(
                "Ochráněný majetek", testClient, realEstateType, 100
        );

        mockMvc.perform(delete(
                "/api/v1/clients/{clientUid}/assets/{assetId}", testClient.getClientUid(), existingAsset.getId()
                ))
                .andExpect(status().isForbidden());
    }
}

