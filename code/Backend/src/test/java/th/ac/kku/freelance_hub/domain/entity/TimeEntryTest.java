package th.ac.kku.freelance_hub.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

@DisplayName("TimeEntry domain tests")
class TimeEntryTest {

    private static final Instant STARTED_AT = Instant.parse("2026-09-22T03:00:00Z");

    private User owner;
    private Client client;
    private Project activeProject;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .email("owner@example.com")
                .passwordHash("hashed-password")
                .build();
        client = new Client(owner, "Example Client");
        activeProject = new Project(owner, client, "Active Project");
        activeProject.changeStatus(ProjectStatus.ACTIVE);
    }

    @Test
    @DisplayName("starts a timer for an active project")
    void startsTimerForActiveProject() {
        Task task = new Task(activeProject, "Implement timer", 0);

        TimeEntry entry = TimeEntry.startTimer(
                owner,
                activeProject,
                task,
                "  Work on timer  ",
                STARTED_AT
        );

        assertThat(entry.getOwner()).isSameAs(owner);
        assertThat(entry.getProject()).isSameAs(activeProject);
        assertThat(entry.getTask()).isSameAs(task);
        assertThat(entry.getDescription()).isEqualTo("Work on timer");
        assertThat(entry.getEntryType()).isEqualTo(EntryType.TIMER);
        assertThat(entry.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(entry.getEndedAt()).isNull();
        assertThat(entry.getDurationMinutes()).isNull();
        assertThat(entry.isRunning()).isTrue();
        assertThat(entry.isLocked()).isFalse();
    }

    @Test
    @DisplayName("rejects starting a timer for a project that is not active")
    void rejectsTimerForInactiveProject() {
        Project plannedProject = new Project(owner, client, "Planned Project");

        assertThatThrownBy(() -> TimeEntry.startTimer(
                owner,
                plannedProject,
                null,
                null,
                STARTED_AT
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("project must be active to track time");
    }

    @Test
    @DisplayName("rejects a project owned by another user")
    void rejectsProjectOwnedByAnotherUser() {
        User anotherOwner = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .email("another@example.com")
                .passwordHash("hashed-password")
                .build();

        assertThatThrownBy(() -> TimeEntry.startTimer(
                anotherOwner,
                activeProject,
                null,
                null,
                STARTED_AT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("project must belong to owner");
    }

    @Test
    @DisplayName("rejects a task from another project")
    void rejectsTaskFromAnotherProject() {
        Project anotherProject = new Project(owner, client, "Another Project");
        Task taskFromAnotherProject = new Task(anotherProject, "Other task", 0);

        assertThatThrownBy(() -> TimeEntry.startTimer(
                owner,
                activeProject,
                taskFromAnotherProject,
                null,
                STARTED_AT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task must belong to project");
    }

    @Test
    @DisplayName("stops a running timer and rounds partial minutes up")
    void stopsTimerAndCalculatesDuration() {
        TimeEntry entry = TimeEntry.startTimer(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT
        );
        Instant endedAt = STARTED_AT.plusSeconds(61);

        entry.stop(endedAt);

        assertThat(entry.getEndedAt()).isEqualTo(endedAt);
        assertThat(entry.getDurationMinutes()).isEqualTo(2);
        assertThat(entry.isRunning()).isFalse();
    }

    @Test
    @DisplayName("rejects stopping a timer more than once")
    void rejectsStoppingTimerTwice() {
        TimeEntry entry = TimeEntry.startTimer(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT
        );
        entry.stop(STARTED_AT.plusSeconds(60));

        assertThatThrownBy(() -> entry.stop(STARTED_AT.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("timer is not running");
    }

    @Test
    @DisplayName("creates a completed manual entry for a non-active project")
    void createsManualEntryForNonActiveProject() {
        Project completedProject = new Project(owner, client, "Completed Project");
        completedProject.changeStatus(ProjectStatus.ACTIVE);
        completedProject.changeStatus(ProjectStatus.COMPLETED);
        Instant endedAt = STARTED_AT.plusSeconds(30 * 60);

        TimeEntry entry = TimeEntry.createManual(
                owner,
                completedProject,
                null,
                "Past work",
                STARTED_AT,
                endedAt
        );

        assertThat(entry.getEntryType()).isEqualTo(EntryType.MANUAL);
        assertThat(entry.getEndedAt()).isEqualTo(endedAt);
        assertThat(entry.getDurationMinutes()).isEqualTo(30);
        assertThat(entry.isRunning()).isFalse();
    }

    @Test
    @DisplayName("creates a manual entry from a positive duration")
    void createsManualEntryFromDuration() {
        TimeEntry entry = TimeEntry.createManualWithDuration(
                owner,
                activeProject,
                null,
                "Timed manually",
                STARTED_AT,
                45
        );

        assertThat(entry.getEntryType()).isEqualTo(EntryType.MANUAL);
        assertThat(entry.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(entry.getEndedAt()).isEqualTo(STARTED_AT.plusSeconds(45 * 60));
        assertThat(entry.getDurationMinutes()).isEqualTo(45);
        assertThat(entry.isRunning()).isFalse();
    }

    @Test
    @DisplayName("rejects a zero manual duration")
    void rejectsZeroManualDuration() {
        assertThatThrownBy(() -> TimeEntry.createManualWithDuration(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT,
                0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("durationMinutes must be greater than zero");
    }

    @Test
    @DisplayName("rejects a negative manual duration")
    void rejectsNegativeManualDuration() {
        assertThatThrownBy(() -> TimeEntry.createManualWithDuration(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT,
                -1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("durationMinutes must be greater than zero");
    }

    @Test
    @DisplayName("rejects a null start time for a manual duration")
    void rejectsNullStartTimeForManualDuration() {
        assertThatThrownBy(() -> TimeEntry.createManualWithDuration(
                owner,
                activeProject,
                null,
                null,
                null,
                30
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("startedAt is required");
    }

    @Test
    @DisplayName("rejects an end time that is not after the start time")
    void rejectsInvalidTimeRange() {
        assertThatThrownBy(() -> TimeEntry.createManual(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT,
                STARTED_AT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endedAt must be after startedAt");
    }

    @Test
    @DisplayName("rejects an end time before the start time")
    void rejectsNegativeDuration() {
        assertThatThrownBy(() -> TimeEntry.createManual(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT,
                STARTED_AT.minusSeconds(60)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endedAt must be after startedAt");
    }

    @Test
    @DisplayName("recalculates duration when a completed time range is updated")
    void updatesCompletedTimeRange() {
        TimeEntry entry = TimeEntry.createManual(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT,
                STARTED_AT.plusSeconds(60)
        );

        entry.updateTimeRange(
                STARTED_AT.plusSeconds(120),
                STARTED_AT.plusSeconds(301)
        );

        assertThat(entry.getStartedAt()).isEqualTo(STARTED_AT.plusSeconds(120));
        assertThat(entry.getEndedAt()).isEqualTo(STARTED_AT.plusSeconds(301));
        assertThat(entry.getDurationMinutes()).isEqualTo(4);
    }

    @Test
    @DisplayName("prevents changes after a time entry is locked")
    void preventsChangesAfterLocking() {
        TimeEntry entry = TimeEntry.createManual(
                owner,
                activeProject,
                null,
                "Original description",
                STARTED_AT,
                STARTED_AT.plusSeconds(60)
        );
        entry.lock(STARTED_AT.plusSeconds(120));

        assertThat(entry.isLocked()).isTrue();
        assertThatThrownBy(() -> entry.updateDetails(
                activeProject,
                null,
                "Changed description"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("locked time entry cannot be changed");
        assertThatThrownBy(() -> entry.updateTimeRange(
                STARTED_AT,
                STARTED_AT.plusSeconds(120)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("locked time entry cannot be changed");
    }

    @Test
    @DisplayName("rejects locking a running timer")
    void rejectsLockingRunningTimer() {
        TimeEntry entry = TimeEntry.startTimer(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT
        );

        assertThatThrownBy(() -> entry.lock(STARTED_AT.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("a running timer cannot be locked");
    }
}
