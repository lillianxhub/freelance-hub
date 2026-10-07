package th.ac.kku.freelance_hub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import jakarta.persistence.EntityManager;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;

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

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private EntityManager entityManager;

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
                .andExpect(jsonPath("$.data.projectId").doesNotExist())
                .andExpect(jsonPath("$.data.projectName").doesNotExist())
                .andExpect(jsonPath("$.data.project.id").value(projectId.toString()))
                .andExpect(jsonPath("$.data.project.name").value("Timer project"))
                .andExpect(jsonPath("$.data.taskId").doesNotExist())
                .andExpect(jsonPath("$.data.taskName").doesNotExist())
                .andExpect(jsonPath("$.data.task.id").value(taskId.toString()))
                .andExpect(jsonPath("$.data.task.title").value("Implementation"))
                .andExpect(jsonPath("$.data.task.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.entryType").doesNotExist())
                .andExpect(jsonPath("$.data.running").doesNotExist())
                .andExpect(jsonPath("$.data.endedAt").doesNotExist())
                .andReturn();
        UUID timerId = UUID.fromString(
                responseJson(started).path("data").path("id").asText()
        );

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/timer/current")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(true))
                .andExpect(jsonPath("$.data.timeEntry.id")
                        .value(timerId.toString()))
                .andExpect(jsonPath("$.data.timeEntry.project.id")
                        .value(projectId.toString()))
                .andExpect(jsonPath("$.data.timeEntry.project.name")
                        .value("Timer project"))
                .andExpect(jsonPath("$.data.timeEntry.task.id")
                        .value(taskId.toString()))
                .andExpect(jsonPath("$.data.timeEntry.task.title")
                        .value("Implementation"))
                .andExpect(jsonPath("$.data.timeEntry.startedAt")
                        .value(TIMER_STARTED_AT.toString()))
                .andExpect(jsonPath("$.data.timeEntry.description")
                        .value("Work in progress"))
                .andExpect(jsonPath("$.data.timeEntry.projectId")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.timeEntry.running")
                        .doesNotExist());

        mockMvc.perform(post("/api/timer/start")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("projectId", projectId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TIMER_ALREADY_RUNNING"));

        clock.advance(Duration.ofMinutes(2));

        mockMvc.perform(post("/api/timer/stop")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("หยุดจับเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data.id")
                        .value(timerId.toString()))
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.endedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.durationSeconds").value(120));

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id")
                        .value(timerId.toString()))
                .andExpect(jsonPath("$.data[0].description")
                        .value("Work in progress"));

        mockMvc.perform(get("/api/timer/current")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(false))
                .andExpect(jsonPath("$.data.timeEntry").doesNotExist());

        mockMvc.perform(post("/api/timer/start")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("projectId", projectId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.taskId").doesNotExist())
                .andExpect(jsonPath("$.data.task").doesNotExist());

        mockMvc.perform(delete("/api/timer/current")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("ยกเลิกการจับเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        mockMvc.perform(get("/api/timer/current")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(false))
                .andExpect(jsonPath("$.data.timeEntry").doesNotExist());
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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("เพิ่มรายการเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data.project.id")
                        .value(projectId.toString()))
                .andExpect(jsonPath("$.data.project.name")
                        .value("Manual project"))
                .andExpect(jsonPath("$.data.task.id")
                        .value(taskId.toString()))
                .andExpect(jsonPath("$.data.task.title").value("Design"))
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.endedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.durationSeconds").value(5400))
                .andExpect(jsonPath("$.data.description")
                        .value("Initial design"))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andReturn();
        UUID entryId = UUID.fromString(
                responseJson(created).path("data").path("id").asText()
        );

        mockMvc.perform(put("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "projectId", projectId,
                                "description", "Updated design",
                                "startedAt", STARTED_AT,
                                "endedAt", ENDED_AT
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description")
                        .value("Updated design"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.task").doesNotExist());

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("projectId", projectId.toString())
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-02T00:00:00Z")
                        .param("sortBy", "startedAt")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("ดึงข้อมูลรายการเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data[0].id")
                        .value(entryId.toString()))
                .andExpect(jsonPath("$.data[0].project.id")
                        .value(projectId.toString()))
                .andExpect(jsonPath("$.data[0].project.name")
                        .value("Manual project"))
                .andExpect(jsonPath("$.data[0].task").value(nullValue()))
                .andExpect(jsonPath("$.data[0].startedAt").isNotEmpty())
                .andExpect(jsonPath("$.data[0].endedAt").isNotEmpty())
                .andExpect(jsonPath("$.data[0].durationSeconds")
                        .value(5400))
                .andExpect(jsonPath("$.data[0].description")
                        .value("Updated design"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(20))
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.error").value(nullValue()));

        mockMvc.perform(get("/api/time-entries/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("projectId", projectId.toString())
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entryCount").value(1))
                .andExpect(jsonPath("$.data.totalSeconds").value(5400))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        mockMvc.perform(delete("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("ลบรายการเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(20))
                .andExpect(jsonPath("$.meta.total").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(0));
    }

    @Test
    void completingProjectLocksItsTimeEntryAndRejectsChanges() throws Exception {
        String token = registerAndGetToken("project-lock-flow@example.com");
        UUID projectId = createActiveProject(token, "Lock project");

        MvcResult created = mockMvc.perform(post("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualEntryJson(projectId, "Completed work")))
                .andExpect(status().isCreated())
                .andReturn();
        UUID entryId = responseId(created);
        assertThat(timeEntryRepository.findById(entryId).orElseThrow().getLockedAt())
                .isNull();

        mockMvc.perform(patch("/api/projects/{id}/status", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        entityManager.clear();
        assertThat(timeEntryRepository.findById(entryId).orElseThrow().getLockedAt())
                .isEqualTo(clock.instant());

        mockMvc.perform(put("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualEntryJson(projectId, "Changed work")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TIME_ENTRY_LOCKED"));

        mockMvc.perform(delete("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TIME_ENTRY_LOCKED"));

        mockMvc.perform(get("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description")
                        .value("Completed work"));
        assertThat(timeEntryRepository.findById(entryId).orElseThrow().getLockedAt())
                .isEqualTo(clock.instant());
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
        UUID entryId = UUID.fromString(
                responseJson(created).path("data").path("id").asText()
        );

        mockMvc.perform(post("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualEntryJson(ownerProjectId, "Stolen work")))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "projectId", ownerProjectId,
                                "description", "Changed",
                                "startedAt", STARTED_AT,
                                "endedAt", ENDED_AT
                        ))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/time-entries/{id}", entryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.total").value(0));

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id")
                        .value(entryId.toString()))
                .andExpect(jsonPath("$.meta.total").value(1));
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
                                "durationSeconds", 5400
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/time-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("from", "2026-09-02T00:00:00Z")
                        .param("to", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private String registerAndGetToken(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "password123",
                                "displayName", "Time entry integration user"
                        ))))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        return responseJson(result).path("data").path("token").asText();
    }

    private UUID createActiveProject(String token, String projectName)
            throws Exception {
        MvcResult client = mockMvc.perform(post("/api/clients")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", projectName + " client"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID clientId = UUID.fromString(responseJson(client).path("data").path("id").asText());

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
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
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
                "durationSeconds", 1800
        ));
    }

    private UUID responseId(MvcResult result) throws Exception {
        JsonNode response = responseJson(result);
        JsonNode data = response.path("data");
        return UUID.fromString((data.hasNonNull("id") ? data : response)
                .path("id").asText());
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
