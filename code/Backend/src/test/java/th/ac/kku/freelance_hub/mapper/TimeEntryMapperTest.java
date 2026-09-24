package th.ac.kku.freelance_hub.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;

@DisplayName("TimeEntry mapper tests")
class TimeEntryMapperTest {

    private static final Instant STARTED_AT = Instant.parse("2026-09-24T03:00:00Z");

    private final TimeEntryMapper mapper = new TimeEntryMapper();

    private Client client;
    private Project project;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("owner@example.com")
                .passwordHash("hashed-password")
                .build();
        client = new Client(owner, "Example Client");
        project = new Project(owner, client, "Example Project");
        project.changeStatus(ProjectStatus.ACTIVE);

        ReflectionTestUtils.setField(client, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(project, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("maps a completed entry and its related data")
    void mapsCompletedEntryWithRelatedData() {
        Task task = new Task(project, "Implement timer", 0);
        UUID taskId = UUID.randomUUID();
        ReflectionTestUtils.setField(task, "id", taskId);

        TimeEntry entry = TimeEntry.createManual(
                project.getOwner(),
                project,
                task,
                "Implement API",
                STARTED_AT,
                STARTED_AT.plusSeconds(90)
        );
        UUID entryId = UUID.randomUUID();
        Instant createdAt = STARTED_AT.minusSeconds(60);
        Instant updatedAt = STARTED_AT.plusSeconds(120);
        setPersistenceFields(entry, entryId, createdAt, updatedAt, 3L);

        TimeEntryResponse response = mapper.toResponse(entry);

        assertThat(response.getId()).isEqualTo(entryId);
        assertThat(response.getClientId()).isEqualTo(client.getId());
        assertThat(response.getClientName()).isEqualTo("Example Client");
        assertThat(response.getProjectId()).isEqualTo(project.getId());
        assertThat(response.getProjectName()).isEqualTo("Example Project");
        assertThat(response.getTaskId()).isEqualTo(taskId);
        assertThat(response.getTaskName()).isEqualTo("Implement timer");
        assertThat(response.getDescription()).isEqualTo("Implement API");
        assertThat(response.getEntryType()).isEqualTo(EntryType.MANUAL);
        assertThat(response.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(response.getEndedAt()).isEqualTo(STARTED_AT.plusSeconds(90));
        assertThat(response.getDurationMinutes()).isEqualTo(2);
        assertThat(response.isRunning()).isFalse();
        assertThat(response.isLocked()).isFalse();
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(response.getVersion()).isEqualTo(3L);
    }

    @Test
    @DisplayName("maps a running timer without a task")
    void mapsRunningTimerWithoutTask() {
        TimeEntry entry = TimeEntry.startTimer(
                project.getOwner(),
                project,
                null,
                null,
                STARTED_AT
        );

        TimeEntryResponse response = mapper.toResponse(entry);

        assertThat(response.getTaskId()).isNull();
        assertThat(response.getTaskName()).isNull();
        assertThat(response.getEndedAt()).isNull();
        assertThat(response.getDurationMinutes()).isNull();
        assertThat(response.isRunning()).isTrue();
        assertThat(response.isLocked()).isFalse();
    }

    @Test
    @DisplayName("maps the locked state and lock time")
    void mapsLockedEntry() {
        TimeEntry entry = TimeEntry.createManual(
                project.getOwner(),
                project,
                null,
                null,
                STARTED_AT,
                STARTED_AT.plusSeconds(60)
        );
        Instant lockedAt = STARTED_AT.plusSeconds(120);
        entry.lock(lockedAt);

        TimeEntryResponse response = mapper.toResponse(entry);

        assertThat(response.getLockedAt()).isEqualTo(lockedAt);
        assertThat(response.isLocked()).isTrue();
        assertThat(response.isRunning()).isFalse();
    }

    @Test
    @DisplayName("rejects a null entity")
    void rejectsNullEntity() {
        assertThatThrownBy(() -> mapper.toResponse(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("entry is required");
    }

    private static void setPersistenceFields(
            TimeEntry entry,
            UUID id,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        ReflectionTestUtils.setField(entry, "id", id);
        ReflectionTestUtils.setField(entry, "createdAt", createdAt);
        ReflectionTestUtils.setField(entry, "updatedAt", updatedAt);
        ReflectionTestUtils.setField(entry, "version", version);
    }
}
