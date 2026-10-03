package th.ac.kku.freelance_hub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;
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
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest;
import th.ac.kku.freelance_hub.dto.request.user.UpdateUserProfileRequest;
import th.ac.kku.freelance_hub.dto.response.user.UserResponse;
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClientIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EntityManager entityManager;

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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void openApiDocumentsClientEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users/me'].get.tags[0]").value("Authentication"))
                .andExpect(jsonPath("$.paths['/api/users/me'].patch.tags[0]").value("Authentication"))
                .andExpect(jsonPath("$.paths['/api/users/me/password'].patch.tags[0]").value("Authentication"))
                .andExpect(jsonPath("$.components.schemas.UpdateUserProfileRequest.properties.profileImageUrl")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.UserResponse.properties.avatarUrl").doesNotExist())
                .andExpect(
                        jsonPath("$.paths['/api/clients'].post.responses['201'].description").value("Client created"))
                .andExpect(jsonPath("$.paths['/api/clients'].get.responses['200'].description")
                        .value("Page of clients returned"))
                .andExpect(jsonPath("$.paths['/api/clients/{id}'].get.responses['404'].description")
                        .value("Client not found"))
                .andExpect(jsonPath("$.paths['/api/clients/{id}'].get.responses['404'].content.*.schema['$ref']")
                        .value(org.hamcrest.Matchers.hasItem("#/components/schemas/ApiResult")))
                .andExpect(jsonPath("$.paths['/api/clients/{id}'].patch.responses['200'].description")
                        .value("Client updated"))
                .andExpect(jsonPath("$.paths['/api/clients/{id}'].delete.responses['204'].description")
                        .value("Client soft-deleted"));
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
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Phone Match"))
                .andExpect(jsonPath("$.data[0].phone").value("0812345678"));

        mockMvc.perform(get("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .param("page", "1")
                .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(1))
                .andExpect(jsonPath("$.meta.total").value(2))
                .andExpect(jsonPath("$.data.length()").value(1));
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
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Address Match"))
                .andExpect(jsonPath("$.data[0].address").value("123 Main Road"));
    }

    @Test
    void clientDetailIncludesOnlyRequestedProjectsAndTasks() throws Exception {
        String ownerToken = registerAndGetToken("client-include-owner@example.com");
        String otherToken = registerAndGetToken("client-include-other@example.com");
        String clientBody = mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Included Client\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID clientId = UUID.fromString(objectMapper.readTree(clientBody).path("data").path("id").asText());

        String projectBody = mockMvc.perform(post("/api/projects")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientId\":\"" + clientId + "\",\"name\":\"Included Project\","
                        + "\"color\":\"#123456\",\"targetMinutes\":120}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID projectId = UUID.fromString(objectMapper.readTree(projectBody).path("data").path("id").asText());

        String taskBody = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Included Task\",\"sortOrder\":0}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID taskId = UUID.fromString(objectMapper.readTree(taskBody).path("data").path("id").asText());

        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects").doesNotExist());
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken))
                .param("include", "projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects[0].id").value(projectId.toString()))
                .andExpect(jsonPath("$.data.projects[0].name").value("Included Project"))
                .andExpect(jsonPath("$.data.projects[0].tasks").doesNotExist());
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken))
                .param("include", "projects,projects.tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects[0].tasks[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$.data.projects[0].tasks[0].name").value("Included Task"));
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken))
                .param("include", "projects.tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects[0].tasks[0].id").value(taskId.toString()));
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken))
                .param("include", "tasks"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken))
                .param("include", "Projects"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(otherToken))
                .param("include", "projects.tasks"))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientGetEndpointsReturnTrackedSecondsMatchingSummaryAndZeroForEmptyClients() throws Exception {
        String token = registerAndGetToken("client-tracked-api@example.com");
        String body = mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Tracked Client\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalTrackedSeconds").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        UUID clientId = UUID.fromString(objectMapper.readTree(body).path("data").path("id").asText());
        Client client = clientRepository.findById(clientId).orElseThrow();
        clientRepository.save(new Client(client.getOwner(), "Zero Client"));
        Project project = new Project(client.getOwner(), client, "Tracked Project");
        project.changeStatus(ProjectStatus.ACTIVE);
        entityManager.persist(project);
        entityManager.persist(TimeEntry.createManualWithDurationSeconds(
                client.getOwner(), project, null, null, Instant.parse("2026-09-01T00:00:00Z"), 5401));
        entityManager.flush();
        entityManager.clear();

        String summaryBody = mockMvc.perform(get("/api/time-entries/summary")
                .header("Authorization", bearer(token))
                .param("clientId", clientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSeconds").value(5401))
                .andReturn().getResponse().getContentAsString();
        long summary = objectMapper.readTree(summaryBody).path("data").path("totalSeconds").asLong();
        mockMvc.perform(get("/api/clients")
                .header("Authorization", bearer(token))
                .param("page", "1").param("limit", "1").param("sortBy", "name").param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.total").value(2))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.data[0].id").value(clientId.toString()))
                .andExpect(jsonPath("$.data[0].totalTrackedSeconds").value(summary));
        mockMvc.perform(get("/api/clients")
                .header("Authorization", bearer(token))
                .param("page", "2").param("limit", "1").param("sortBy", "name").param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Zero Client"))
                .andExpect(jsonPath("$.data[0].totalTrackedSeconds").value(0));
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTrackedSeconds").value(summary));
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(token))
                .param("include", "projects.tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTrackedSeconds").value(summary))
                .andExpect(jsonPath("$.data.projects[0].id").value(project.getId().toString()))
                .andExpect(jsonPath("$.data.projects[0].tasks").isEmpty());
    }

    @Test
    void updatedAddressFieldsAreStoredSeparatelyAndReturnedAfterReload() throws Exception {
        String ownerToken = registerAndGetToken("client-address-update@example.com");
        String createdBody = mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Address Client\",\"address\":\"Old road\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID clientId = UUID.fromString(objectMapper.readTree(createdBody).path("data").path("id").asText());

        mockMvc.perform(patch("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"address\":\"271 moo 8\",\"subdistrict\":\"Tha Hin\","
                        + "\"district\":\"Mueang Lop Buri\",\"province\":\"Lop Buri\","
                        + "\"postalCode\":\"15000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("271 moo 8"))
                .andExpect(jsonPath("$.data.subdistrict").value("Tha Hin"))
                .andExpect(jsonPath("$.data.district").value("Mueang Lop Buri"))
                .andExpect(jsonPath("$.data.province").value("Lop Buri"))
                .andExpect(jsonPath("$.data.postalCode").value("15000"));

        entityManager.flush();
        entityManager.clear();
        var stored = clientRepository.findById(clientId).orElseThrow();
        assertThat(stored.getAddress()).isEqualTo("271 moo 8");
        assertThat(stored.getSubdistrict()).isEqualTo("Tha Hin");
        assertThat(stored.getDistrict()).isEqualTo("Mueang Lop Buri");
        assertThat(stored.getProvince()).isEqualTo("Lop Buri");
        assertThat(stored.getPostalCode()).isEqualTo("15000");

        entityManager.clear();
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("271 moo 8"))
                .andExpect(jsonPath("$.data.subdistrict").value("Tha Hin"))
                .andExpect(jsonPath("$.data.district").value("Mueang Lop Buri"))
                .andExpect(jsonPath("$.data.province").value("Lop Buri"))
                .andExpect(jsonPath("$.data.postalCode").value("15000"));
    }

    @Test
    void clientValidationAndNotFoundErrorsUseApiResult() throws Exception {
        String token = registerAndGetToken("client-error-envelope@example.com");

        mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("ข้อมูลที่ส่งมาไม่ถูกต้อง"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.name").exists());

        mockMvc.perform(get("/api/clients/{id}", UUID.randomUUID())
                .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.error.code").value("CLIENT_NOT_FOUND"));
    }

    @Test
    void ownerCanReplaceClientWithoutChangingOwnershipOrStatus() throws Exception {
        String ownerToken = registerAndGetToken("client-replace-owner@example.com");
        String otherToken = registerAndGetToken("client-replace-other@example.com");
        String createdBody = mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Original\",\"companyName\":\"Acme\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID clientId = UUID.fromString(objectMapper.readTree(createdBody).path("data").path("id").asText());

        mockMvc.perform(put("/api/clients/{id}", clientId)
                .header("Authorization", bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Stolen\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Replaced\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Replaced"))
                .andExpect(jsonPath("$.data.companyName").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void ownerCanArchiveAndReactivateClientThroughStatusEndpoint() throws Exception {
        String ownerToken = registerAndGetToken("client-status-owner@example.com");
        String otherToken = registerAndGetToken("client-status-other@example.com");
        String createdBody = mockMvc.perform(post("/api/clients")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Status Client\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID clientId = UUID.fromString(objectMapper.readTree(createdBody).path("data").path("id").asText());

        mockMvc.perform(patch("/api/clients/{id}/status", clientId)
                .header("Authorization", bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\":false}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/clients/{id}/status", clientId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/clients/{id}/status", clientId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.isActive").value(false));
        assertThat(clientRepository.findById(clientId).orElseThrow().getDeletedAt()).isNull();

        mockMvc.perform(patch("/api/clients/{id}/status", clientId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.isActive").value(true));
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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Acme"))
                .andReturn().getResponse().getContentAsString();
        UUID clientId = UUID.fromString(objectMapper.readTree(createdBody).path("data").path("id").asText());

        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(clientId.toString()))
                .andExpect(jsonPath("$.data.name").value("Acme"));
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/clients")
                .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Acme"));
        mockMvc.perform(delete("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());
        var deletedClient = clientRepository.findById(clientId).orElseThrow();
        assertThat(deletedClient.getIsActive()).isTrue();
        assertThat(deletedClient.getDeletedAt()).isNotNull();
        mockMvc.perform(get("/api/clients/{id}", clientId)
                .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/clients")
                .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(0));
        mockMvc.perform(patch("/api/clients/{id}/status", clientId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\":false}"))
                .andExpect(status().isNotFound());
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
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
