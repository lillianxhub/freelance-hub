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
    void malformedAndRevokedTokensAreRejected() throws Exception {
        mockMvc.perform(get("/api/clients")
                .header("Authorization", "Bearer malformed-token"))
            .andExpect(status().isUnauthorized());

        String token = registerAndGetToken("revoked-client-token@example.com");
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", bearer(token)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/clients")
                .header("Authorization", bearer(token)))
            .andExpect(status().isUnauthorized());
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
