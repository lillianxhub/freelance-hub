package th.ac.kku.freelance_hub.integration;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import th.ac.kku.freelance_hub.dto.request.RegisterRequest;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClientIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void unauthenticatedClientRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/clients"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiDocumentsClientEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/clients'].post.responses['201'].description").value("Client created"))
            .andExpect(jsonPath("$.paths['/api/clients'].get.responses['200'].description").value("Page of clients returned"))
            .andExpect(jsonPath("$.paths['/api/clients/{id}'].get.responses['404'].description").value("Client not found"))
            .andExpect(jsonPath("$.paths['/api/clients/{id}'].patch.responses['200'].description").value("Client updated"))
            .andExpect(jsonPath("$.paths['/api/clients/{id}'].delete.responses['204'].description").value("Client archived"));
    }

    @Test
    void searchByPhoneReturnsOnlyTheCurrentUsersMatchingClients() throws Exception {
        String ownerToken = registerAndGetToken("client-phone-owner@example.com");
        String otherToken = registerAndGetToken("client-phone-other@example.com");

        mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Phone Match\",\"phone\":\"0812345678\"}"))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Different Phone\",\"phone\":\"0999999999\"}"))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Other Owner\",\"phone\":\"0812999999\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .param("search", "0812"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Phone Match"))
            .andExpect(jsonPath("$.content[0].phone").value("0812345678"));
    }

    @Test
    void searchByAddressReturnsOnlyTheCurrentUsersMatchingClients() throws Exception {
        String ownerToken = registerAndGetToken("client-address-owner@example.com");
        String otherToken = registerAndGetToken("client-address-other@example.com");

        mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Address Match\",\"address\":\"123 Main Road\"}"))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Other Owner\",\"address\":\"123 Other Road\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .param("search", "123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Address Match"))
            .andExpect(jsonPath("$.content[0].address").value("123 Main Road"));
    }

    @Test
    void ownerCanManageClientWhileAnotherUserCannotReadOrChangeIt() throws Exception {
        String ownerToken = registerAndGetToken("client-integration-owner@example.com");
        String otherToken = registerAndGetToken("client-integration-other@example.com");

        String createdBody = mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Acme\",\"email\":\"acme@example.com\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Acme"))
            .andReturn().getResponse().getContentAsString();
        UUID clientId = UUID.fromString(objectMapper.readTree(createdBody).path("id").asText());

        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(otherToken)))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/clients")
                .header("Authorization", bearer(otherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(patch("/api/clients/{id}", clientId)
                .header("Authorization", bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Stolen\"}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/clients/{id}", clientId)
                .header("Authorization", bearer(otherToken)))
            .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Acme\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Acme"));
        mockMvc.perform(delete("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Acme"))
            .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .email(email)
            .password("password123")
            .displayName("Client integration user")
            .build();
        String body = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
