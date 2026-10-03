package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.request.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.exception.TimeEntryLockedException;
import th.ac.kku.freelance_hub.exception.TimeEntryNotFoundException;
import th.ac.kku.freelance_hub.mapper.TimeEntryMapper;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.impl.TimeEntryServiceImpl;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceImplTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();
    private static final UUID ENTRY_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-26T08:30:00Z");

    @Mock
    private TimeEntryRepository timeEntryRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;

    private TimeEntryServiceImpl service;
    private User owner;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(OWNER_ID)
                .email("time-entry-owner@example.com")
                .passwordHash("test-hash")
                .build();
        Client client = new Client(owner, "Time Entry Client");
        ReflectionTestUtils.setField(client, "id", UUID.randomUUID());
        project = new Project(owner, client, "Time Entry Project");
        project.changeStatus(ProjectStatus.ACTIVE);
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        task = new Task(project, "Time Entry Task", 0);
        ReflectionTestUtils.setField(task, "id", TASK_ID);

        service = new TimeEntryServiceImpl(
                timeEntryRepository,
                projectRepository,
                taskRepository,
                userRepository,
                new TimeEntryMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsManualEntryFromExplicitTimeRange() {
        stubOwnedUserAndProject();
        when(taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                TASK_ID, PROJECT_ID, OWNER_ID
        )).thenReturn(Optional.of(task));
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Instant startedAt = NOW.minusSeconds(90);

        var response = service.createManual(
                OWNER_ID,
                ManualTimeEntryRequest.builder()
                        .projectId(PROJECT_ID)
                        .taskId(TASK_ID)
                        .description("Manual work")
                        .startedAt(startedAt)
                        .endedAt(NOW)
                        .build()
        );

        assertThat(response.getEntryType()).isEqualTo(EntryType.MANUAL);
        assertThat(response.getStartedAt()).isEqualTo(startedAt);
        assertThat(response.getEndedAt()).isEqualTo(NOW);
        assertThat(response.getDurationSeconds()).isEqualTo(90L);
        assertThat(response.getTaskId()).isEqualTo(TASK_ID);
        assertThat(response.isRunning()).isFalse();
        verify(timeEntryRepository).save(any(TimeEntry.class));
        verify(timeEntryRepository, never())
                .saveAndFlush(any(TimeEntry.class));
    }

    @Test
    void createsManualEntryFromDurationForNonActiveProject() {
        Project plannedProject = new Project(
                owner,
                project.getClient(),
                "Historical Project"
        );
        ReflectionTestUtils.setField(plannedProject, "id", PROJECT_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(plannedProject));
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createManual(
                OWNER_ID,
                ManualTimeEntryRequest.builder()
                        .projectId(PROJECT_ID)
                        .description("Historical work")
                        .startedAt(NOW)
                        .durationSeconds(90L)
                        .build()
        );

        assertThat(response.getStartedAt()).isEqualTo(NOW);
        assertThat(response.getEndedAt())
                .isEqualTo(NOW.plusSeconds(90));
        assertThat(response.getDurationSeconds()).isEqualTo(90L);
        assertThat(response.isRunning()).isFalse();
    }

    @Test
    void rejectsManualEntryWithoutEndTimeOrDuration() {
        ManualTimeEntryRequest request = ManualTimeEntryRequest.builder()
                .projectId(PROJECT_ID)
                .startedAt(NOW)
                .build();

        assertThatThrownBy(() -> service.createManual(OWNER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Provide either end time or duration seconds, but not both"
                );

        verifyNoInteractions(userRepository, projectRepository, taskRepository);
        verify(timeEntryRepository, never()).save(any(TimeEntry.class));
    }

    @Test
    void rejectsManualEntryWithBothEndTimeAndDuration() {
        ManualTimeEntryRequest request = ManualTimeEntryRequest.builder()
                .projectId(PROJECT_ID)
                .startedAt(NOW)
                .endedAt(NOW.plusSeconds(60))
                .durationSeconds(60L)
                .build();

        assertThatThrownBy(() -> service.createManual(OWNER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Provide either end time or duration seconds, but not both"
                );

        verify(timeEntryRepository, never()).save(any(TimeEntry.class));
    }

    @Test
    void replacesAllFieldsUsingEndTime() {
        TimeEntry entry = manualEntry(project, task);
        Instant newStartedAt = NOW.minusSeconds(90);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                TASK_ID, PROJECT_ID, OWNER_ID
        )).thenReturn(Optional.of(task));

        var response = service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .projectId(PROJECT_ID)
                        .taskId(TASK_ID)
                        .description("  Updated work  ")
                        .startedAt(newStartedAt)
                        .endedAt(NOW)
                        .build()
        );

        assertThat(response.getDescription()).isEqualTo("Updated work");
        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(response.getTaskId()).isEqualTo(TASK_ID);
        assertThat(response.getStartedAt()).isEqualTo(newStartedAt);
        assertThat(response.getEndedAt()).isEqualTo(NOW);
        assertThat(response.getDurationSeconds()).isEqualTo(90L);
        verify(timeEntryRepository).flush();
    }

    @Test
    void clearsOldTaskWhenProjectChangesWithoutTaskId() {
        TimeEntry entry = manualEntry(project, task);
        UUID newProjectId = UUID.randomUUID();
        Project newProject = new Project(
                owner,
                project.getClient(),
                "New Project"
        );
        ReflectionTestUtils.setField(newProject, "id", newProjectId);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));
        when(projectRepository.findByIdAndOwnerId(newProjectId, OWNER_ID))
                .thenReturn(Optional.of(newProject));

        var response = service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .projectId(newProjectId)
                        .description("Moved work")
                        .startedAt(NOW)
                        .durationSeconds(90L)
                        .build()
        );

        assertThat(response.getProjectId()).isEqualTo(newProjectId);
        assertThat(response.getTaskId()).isNull();
        verifyNoInteractions(taskRepository);
    }

    @Test
    void changesProjectAndTaskTogetherWhenBothAreOwned() {
        TimeEntry entry = manualEntry(project, task);
        UUID newProjectId = UUID.randomUUID();
        UUID newTaskId = UUID.randomUUID();
        Project newProject = new Project(
                owner,
                project.getClient(),
                "New Project"
        );
        ReflectionTestUtils.setField(newProject, "id", newProjectId);
        Task newTask = new Task(newProject, "New Task", 0);
        ReflectionTestUtils.setField(newTask, "id", newTaskId);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));
        when(projectRepository.findByIdAndOwnerId(newProjectId, OWNER_ID))
                .thenReturn(Optional.of(newProject));
        when(taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                newTaskId, newProjectId, OWNER_ID
        )).thenReturn(Optional.of(newTask));

        var response = service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .projectId(newProjectId)
                        .taskId(newTaskId)
                        .description("Moved task")
                        .startedAt(NOW)
                        .durationSeconds(90L)
                        .build()
        );

        assertThat(response.getProjectId()).isEqualTo(newProjectId);
        assertThat(response.getTaskId()).isEqualTo(newTaskId);
    }

    @Test
    void clearsTaskAndDescriptionWhenTheyAreNull() {
        TimeEntry entry = manualEntry(project, task);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));

        var response = service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .projectId(PROJECT_ID)
                        .startedAt(NOW)
                        .durationSeconds(90L)
                        .build()
        );

        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(response.getTaskId()).isNull();
        assertThat(response.getDescription()).isNull();
        assertThat(response.getEndedAt()).isEqualTo(NOW.plusSeconds(90));
        assertThat(response.getDurationSeconds()).isEqualTo(90L);
    }

    @Test
    void updatesCompletedEntryTimeRangeAndRecalculatesDuration() {
        TimeEntry entry = manualEntry(project, task);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        Instant newStart = NOW.minusSeconds(5 * 60);

        var response = service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .projectId(PROJECT_ID)
                        .startedAt(newStart)
                        .endedAt(NOW)
                        .build()
        );

        assertThat(response.getStartedAt()).isEqualTo(newStart);
        assertThat(response.getEndedAt()).isEqualTo(NOW);
        assertThat(response.getDurationSeconds()).isEqualTo(300L);
    }

    @Test
    void rejectsUpdateOfLockedEntry() {
        TimeEntry entry = manualEntry(project, task);
        entry.lock(NOW);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .projectId(PROJECT_ID)
                        .description("Must not change")
                        .startedAt(NOW)
                        .durationSeconds(60L)
                        .build()
        )).isInstanceOf(TimeEntryLockedException.class);

        assertThat(entry.getDescription()).isEqualTo("Original work");
    }

    @Test
    void rejectsUpdateOfEntryNotOwnedByUser() {
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .projectId(PROJECT_ID)
                        .description("Must not change")
                        .startedAt(NOW)
                        .durationSeconds(60L)
                        .build()
        )).isInstanceOf(TimeEntryNotFoundException.class);
    }

    @Test
    void rejectsUpdateWithoutRequiredFields() {
        assertThatThrownBy(() -> service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder().build()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project ID is required");

        verify(timeEntryRepository, never())
                .findByIdAndOwnerIdAndIsActiveTrue(any(), any());
    }

    @Test
    void deletesOwnedUnlockedCompletedEntry() {
        TimeEntry entry = manualEntry(project, task);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));

        service.delete(OWNER_ID, ENTRY_ID);

        assertThat(entry.getIsActive()).isFalse();
        assertThat(entry.getDeletedAt()).isEqualTo(NOW);
        verify(timeEntryRepository).flush();
        verify(timeEntryRepository, never()).delete(any(TimeEntry.class));
    }

    @Test
    void rejectsDeleteOfLockedEntry() {
        TimeEntry entry = manualEntry(project, task);
        entry.lock(NOW);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.delete(OWNER_ID, ENTRY_ID))
                .isInstanceOf(TimeEntryLockedException.class);

        verify(timeEntryRepository, never()).delete(any(TimeEntry.class));
    }

    @Test
    void rejectsDeleteOfRunningTimer() {
        TimeEntry runningTimer = TimeEntry.startTimer(
                owner,
                project,
                null,
                "Current timer",
                NOW
        );
        ReflectionTestUtils.setField(runningTimer, "id", ENTRY_ID);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(runningTimer));

        assertThatThrownBy(() -> service.delete(OWNER_ID, ENTRY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("a running timer must be cancelled");

        verify(timeEntryRepository, never()).delete(any(TimeEntry.class));
    }

    @Test
    void rejectsDeleteOfEntryNotOwnedByUser() {
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(OWNER_ID, ENTRY_ID))
                .isInstanceOf(TimeEntryNotFoundException.class);

        verify(timeEntryRepository, never()).delete(any(TimeEntry.class));
    }


    @Test
    void locksManualCompletedTimerAndSoftDeletedEntryWithTheSameServerTime() {
        TimeEntry manual = manualEntry(project, null);
        TimeEntry timer = TimeEntry.startTimer(
                owner, project, null, "Timer", NOW.minusSeconds(120)
        );
        timer.stop(NOW.minusSeconds(30));
        TimeEntry deleted = manualEntry(project, null);
        deleted.softDelete(NOW.minusSeconds(10));
        when(timeEntryRepository.findLockedByOwnerIdAndProjectIdAndLockedAtIsNull(
                OWNER_ID, PROJECT_ID
        )).thenReturn(List.of(manual, timer, deleted));

        service.lockByProject(OWNER_ID, PROJECT_ID);

        assertThat(List.of(manual, timer, deleted)).allSatisfy(entry -> {
            assertThat(entry.getLockedAt()).isEqualTo(NOW);
            assertThat(entry.isLocked()).isTrue();
        });
        assertThat(deleted.getIsActive()).isFalse();
        assertThat(deleted.getDeletedAt()).isEqualTo(NOW.minusSeconds(10));
        assertThatThrownBy(() -> manual.updateTimeRangeWithDurationSeconds(NOW, 60))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> manual.softDelete(NOW))
                .isInstanceOf(IllegalStateException.class);
        verify(timeEntryRepository).flush();
    }

    @Test
    void rejectsRunningTimerBeforeLockingAnyOtherEntry() {
        TimeEntry manual = manualEntry(project, null);
        TimeEntry running = TimeEntry.startTimer(owner, project, null, "Timer", NOW);
        when(timeEntryRepository.findLockedByOwnerIdAndProjectIdAndLockedAtIsNull(
                OWNER_ID, PROJECT_ID
        )).thenReturn(List.of(manual, running));

        assertThatThrownBy(() -> service.lockByProject(OWNER_ID, PROJECT_ID))
                .isInstanceOf(IllegalStateException.class);

        assertThat(manual.getLockedAt()).isNull();
        assertThat(running.isRunning()).isTrue();
        assertThat(running.getLockedAt()).isNull();
        verify(timeEntryRepository, never()).flush();
    }

    @Test
    void doesNothingWhenNoUnlockedEntriesRemain() {
        when(timeEntryRepository.findLockedByOwnerIdAndProjectIdAndLockedAtIsNull(
                OWNER_ID, PROJECT_ID
        )).thenReturn(List.of());

        service.lockByProject(OWNER_ID, PROJECT_ID);

        verify(timeEntryRepository, never()).flush();
    }

    @Test
    void requiresOwnerAndProjectIdsBeforeLockQuery() {
        assertThatThrownBy(() -> service.lockByProject(null, PROJECT_ID))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.lockByProject(OWNER_ID, null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(timeEntryRepository);
    }

    private void stubOwnedUserAndProject() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
    }

    private TimeEntry manualEntry(Project entryProject, Task entryTask) {
        TimeEntry entry = TimeEntry.createManual(
                owner,
                entryProject,
                entryTask,
                "Original work",
                NOW.minusSeconds(2 * 60),
                NOW.minusSeconds(60)
        );
        ReflectionTestUtils.setField(entry, "id", ENTRY_ID);
        return entry;
    }
}
