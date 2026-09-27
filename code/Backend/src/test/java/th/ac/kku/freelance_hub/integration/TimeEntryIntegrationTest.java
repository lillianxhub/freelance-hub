package th.ac.kku.freelance_hub.integration;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TimeEntryIntegrationTest.TestClockConfiguration.class)
class TimeEntryIntegrationTest {

    private static final String STARTED_AT = "2026-09-01T09:00:00Z";
    private static final String ENDED_AT = "2026-09-01T10:30:00Z";
    private static final Instant TIMER_STARTED_AT =
            Instant.parse("2026-09-27T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AdjustableClock clock;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        clock.set(TIMER_STARTED_AT);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedTimeTrackingRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/time-entries"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/timer/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanRunStopAndCancelTimersWithAnOptionalTask() throws Exception {
        String token = registerAndGetToken("timer-flow@example.com");
        UUID projectId = createActiveProject(token, "Timer project");
        UUID taskId = createTask(token, projectId, "Implementation");

        MvcResult started = mockMvc.perform(post("/api/timer/start")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "projectId", projectId,
                                "taskId", taskId,
                                "description", "Work in progress"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        org.hamcrest.Matchers.startsWith("/api/time-entries/")
                ))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.taskId").value(taskId.toString()))
                .andExpect(jsonPath("$.entryType").value("TIMER"))
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.endedAt").doesNotExist())
                .andReturn();
        UUID timerId = responseId(started);

        mockMvc.perform(get("/api/timer/current")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(timerId.toString()))
                .andExpect(jsonPath("$.running").value(true));

        mockMvc.perform(post("/api/timer/start")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("projectId", projectId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        clock.advance(Duration.ofMinutes(2));

        mockMvc.perform(post("/api/timer/stop")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(timerId.toString()))
                .andExpect(jsonPath("$.running").value(false))
                .andExpect(jsonPath("$.endedAt").isNotEmpty())
                .andExpect(jsonPath("$.durationMinutes").value(2));

        mockMvc.perform(get("/api/timer/current")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/timer/start")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("projectId", projectId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").doesNotExist());

        mockMvc.perform(delete("/api/timer/current")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/timer/current")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanCreateUpdateListSummarizeAndDeleteManualEntry()
            throws Exception {
        String token = registerAndGetToken("manual-flow@example.com");
        UUID projectId = createActiveProject(token, "Manual project");
        UUID taskId = createTask(token, projectId, "Design");

        MvcResult created = mockMvc.perform(post("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "projectId", projectId,
                                "taskId", taskId,
                                "description", "Initial design",
                                "startedAt", STARTED_AT,
                                "endedAt", ENDED_AT
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entryType").value("MANUAL"))
                .andExpect(jsonPath("$.durationMinutes").value(90))
                .andExpect(jsonPath("$.running").value(false))
                .andReturn();
        UUID entryId = responseId(created);

        mockMvc.perform(patch("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "description", "Updated design",
                                "clearTask", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated design"))
                .andExpect(jsonPath("$.taskId").doesNotExist());

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("projectId", projectId.toString())
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-02T00:00:00Z")
                        .param("sortBy", "startedAt")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(entryId.toString()))
                .andExpect(jsonPath("$.content[0].description")
                        .value("Updated design"));

        mockMvc.perform(get("/api/time-entries/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("projectId", projectId.toString())
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(1))
                .andExpect(jsonPath("$.totalMinutes").value(90));

        mockMvc.perform(delete("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void timeEntriesAreIsolatedByAuthenticatedOwner() throws Exception {
        String ownerToken = registerAndGetToken("entry-owner@example.com");
        String otherToken = registerAndGetToken("entry-other@example.com");
        UUID ownerProjectId = createActiveProject(ownerToken, "Private project");

        MvcResult created = mockMvc.perform(post("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualEntryJson(ownerProjectId, "Private work")))
                .andExpect(status().isCreated())
                .andReturn();
        UUID entryId = responseId(created);

        mockMvc.perform(post("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualEntryJson(ownerProjectId, "Stolen work")))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("description", "Changed"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description")
                        .value("Private work"));
    }

    @Test
    void invalidManualTimeAndInvalidFilterAreRejected() throws Exception {
        String token = registerAndGetToken("entry-validation@example.com");
        UUID projectId = createActiveProject(token, "Validation project");

        mockMvc.perform(post("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "projectId", projectId,
                                "startedAt", STARTED_AT,
                                "endedAt", ENDED_AT,
                                "durationMinutes", 90
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("from", "2026-09-02T00:00:00Z")
                        .param("to", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "password123",
                                "displayName", "Time entry integration user"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return responseJson(result).path("token").asText();
    }

    private UUID createActiveProject(String token, String projectName)
            throws Exception {
        MvcResult client = mockMvc.perform(post("/api/clients")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", projectName + " client"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID clientId = responseId(client);

        MvcResult project = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "clientId", clientId,
                                "name", projectName,
                                "targetMinutes", 600
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID projectId = responseId(project);

        mockMvc.perform(patch("/api/projects/{id}/status", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        return projectId;
    }

    private UUID createTask(String token, UUID projectId, String taskName)
            throws Exception {
        MvcResult task = mockMvc.perform(post(
                            "/api/projects/{projectId}/tasks",
                            projectId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", taskName,
                                "sortOrder", 0
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return responseId(task);
    }

    private String manualEntryJson(UUID projectId, String description)
            throws Exception {
        return json(Map.of(
                "projectId", projectId,
                "description", description,
                "startedAt", STARTED_AT,
                "durationMinutes", 30
        ));
    }

    private UUID responseId(MvcResult result) throws Exception {
        return UUID.fromString(responseJson(result).path("id").asText());
    }

    private JsonNode responseJson(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsByteArray()
        );
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @TestConfiguration
    static class TestClockConfiguration {

        @Bean
        @Primary
        AdjustableClock adjustableClock() {
            return new AdjustableClock(TIMER_STARTED_AT);
        }
    }

    static final class AdjustableClock extends Clock {

        private final AtomicReference<Instant> currentInstant;

        private AdjustableClock(Instant initialInstant) {
            currentInstant = new AtomicReference<>(initialInstant);
        }

        void set(Instant instant) {
            currentInstant.set(instant);
        }

        void advance(Duration duration) {
            currentInstant.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException("Test clock uses UTC");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant.get();
        }
    }
}
