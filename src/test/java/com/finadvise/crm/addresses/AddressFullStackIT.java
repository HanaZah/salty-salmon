package com.finadvise.crm.addresses;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class AddressFullStackIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldExecuteFullFlowFromApiToOracle() throws Exception {
        var requestDto = new AddressDTO(null, "Thákurova", "6", "Praha", "100 00");

        var result = mockMvc.perform(post("/api/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AddressDTO responseDto = objectMapper.readValue(result, AddressDTO.class);
        assertThat(responseDto.street()).isEqualTo("Thákurova");
        assertThat(responseDto.id()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_ReturnsAddress_WhenItExists() throws Exception {
        var requestDto = new AddressDTO(null, "Vodičkova", "15", "Praha", "110 00");
        String createResult = mockMvc.perform(post("/api/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AddressDTO createdAddress = objectMapper.readValue(createResult, AddressDTO.class);

        mockMvc.perform(get("/api/v1/addresses/" + createdAddress.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Vodičkova"));
    }

    @Test
    @WithMockUser(roles = "ADVISOR")
    void getById_Returns404_WhenAddressDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/addresses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_Returns400_WhenValidationFails() throws Exception {
        // Bad PSC format
        var requestDto = new AddressDTO(null, "Thákurova", "6", "Praha", "10000");

        mockMvc.perform(post("/api/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @WithMockUser(roles = "ADVISOR")
    void create_Returns403_WhenUserIsAdvisor() throws Exception {
        var requestDto = new AddressDTO(null, "Thákurova", "6", "Praha", "100 00");

        mockMvc.perform(post("/api/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }
}