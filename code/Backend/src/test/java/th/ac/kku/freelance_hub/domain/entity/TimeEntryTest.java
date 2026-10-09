package th.ac.kku.freelance_hub.domain.entity;

import th.ac.kku.freelance_hub.exception.InvalidStateException;
import th.ac.kku.freelance_hub.exception.InvalidArgumentException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
        assertThat(entry.getDurationSeconds()).isNull();
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
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("โปรเจกต์ต้องอยู่ในสถานะกำลังทำก่อนบันทึกเวลา");
    }

    @Test
    @DisplayName("rejects starting a timer for an archived client")
    void rejectsTimerForArchivedClient() {
        client.setActive(false);

        assertThatThrownBy(() -> TimeEntry.startTimer(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT
        ))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("กรุณาคืนสถานะลูกค้าก่อนบันทึกเวลา");
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
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("โปรเจกต์ไม่ถูกต้อง");
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
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("งานไม่อยู่ในโปรเจกต์ที่เลือก");
    }

    @Test
    @DisplayName("stops a running timer and stores exact elapsed seconds")
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
        assertThat(entry.getDurationSeconds()).isEqualTo(61L);
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
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("ตัวจับเวลาไม่ได้กำลังทำงาน");
    }

    @ParameterizedTest
    @EnumSource(value = ProjectStatus.class, names = {
            "PLANNED", "ON_HOLD", "COMPLETED", "ARCHIVED"
    })
    @DisplayName("rejects manual entries for a project that cannot track time")
    void rejectsManualEntryForNonActiveProject(ProjectStatus status) {
        Project nonActiveProject = new Project(owner, client, "Non-active Project");
        if (status == ProjectStatus.ON_HOLD || status == ProjectStatus.COMPLETED) {
            nonActiveProject.changeStatus(ProjectStatus.ACTIVE);
        }
        nonActiveProject.changeStatus(status);
        Instant endedAt = STARTED_AT.plusSeconds(30 * 60);

        assertThatThrownBy(() -> TimeEntry.createManual(
                owner,
                nonActiveProject,
                null,
                "Past work",
                STARTED_AT,
                endedAt
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("project must be active to track time");
        assertThatThrownBy(() -> TimeEntry.createManualWithDurationSeconds(
                owner,
                nonActiveProject,
                null,
                "Past work",
                STARTED_AT,
                1800
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("project must be active to track time");
    }

    @Test
    @DisplayName("stores an exact manual duration in seconds")
    void createsManualEntryFromDuration() {
        TimeEntry entry = TimeEntry.createManualWithDurationSeconds(
                owner,
                activeProject,
                null,
                "Timed manually",
                STARTED_AT,
                90
        );

        assertThat(entry.getEntryType()).isEqualTo(EntryType.MANUAL);
        assertThat(entry.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(entry.getEndedAt()).isEqualTo(STARTED_AT.plusSeconds(90));
        assertThat(entry.getDurationSeconds()).isEqualTo(90L);
        assertThat(entry.isRunning()).isFalse();
    }

    @Test
    @DisplayName("rejects a zero manual duration")
    void rejectsZeroManualDuration() {
        assertThatThrownBy(() -> TimeEntry.createManualWithDurationSeconds(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT,
                0
        ))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("ระยะเวลาต้องมากกว่าศูนย์");
    }

    @Test
    @DisplayName("rejects a negative manual duration")
    void rejectsNegativeManualDuration() {
        assertThatThrownBy(() -> TimeEntry.createManualWithDurationSeconds(
                owner,
                activeProject,
                null,
                null,
                STARTED_AT,
                -1
        ))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("ระยะเวลาต้องมากกว่าศูนย์");
    }

    @Test
    @DisplayName("rejects a null start time for a manual duration")
    void rejectsNullStartTimeForManualDuration() {
        assertThatThrownBy(() -> TimeEntry.createManualWithDurationSeconds(
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
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่มต้น");
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
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่มต้น");
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
        assertThat(entry.getDurationSeconds()).isEqualTo(181L);
    }

    @Test
    @DisplayName("rejects moving an existing entry to a non-active project")
    void rejectsUpdatingDetailsToNonActiveProject() {
        TimeEntry entry = TimeEntry.createManual(
                owner, activeProject, null, "Original work",
                STARTED_AT, STARTED_AT.plusSeconds(60)
        );
        Project plannedProject = new Project(owner, client, "Planned Project");

        assertThatThrownBy(() -> entry.updateDetails(
                plannedProject, null, "Changed work"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("project must be active to track time");
        assertThat(entry.getProject()).isSameAs(activeProject);
        assertThat(entry.getDescription()).isEqualTo("Original work");
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
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("รายการเวลาถูกล็อกแล้ว ไม่สามารถแก้ไขได้");
        assertThatThrownBy(() -> entry.updateTimeRange(
                STARTED_AT,
                STARTED_AT.plusSeconds(120)
        ))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("รายการเวลาถูกล็อกแล้ว ไม่สามารถแก้ไขได้");
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
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("กรุณาหยุดตัวจับเวลาก่อนล็อกรายการเวลา");
    }
}
