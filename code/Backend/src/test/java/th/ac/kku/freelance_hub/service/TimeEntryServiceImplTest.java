package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
                new TimeEntryMapper()
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
        assertThat(response.getDurationMinutes()).isEqualTo(2);
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
                        .durationMinutes(30)
                        .build()
        );

        assertThat(response.getStartedAt()).isEqualTo(NOW);
        assertThat(response.getEndedAt())
                .isEqualTo(NOW.plusSeconds(30 * 60));
        assertThat(response.getDurationMinutes()).isEqualTo(30);
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
                        "Provide either end time or duration minutes, but not both"
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
                .durationMinutes(1)
                .build();

        assertThatThrownBy(() -> service.createManual(OWNER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Provide either end time or duration minutes, but not both"
                );

        verify(timeEntryRepository, never()).save(any(TimeEntry.class));
    }

    @Test
    void updatesDescriptionWithoutChangingProjectTaskOrTime() {
        TimeEntry entry = manualEntry(project, task);
        Instant originalStartedAt = entry.getStartedAt();
        Instant originalEndedAt = entry.getEndedAt();
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));

        var response = service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .description("  Updated work  ")
                        .build()
        );

        assertThat(response.getDescription()).isEqualTo("Updated work");
        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(response.getTaskId()).isEqualTo(TASK_ID);
        assertThat(response.getStartedAt()).isEqualTo(originalStartedAt);
        assertThat(response.getEndedAt()).isEqualTo(originalEndedAt);
        verifyNoInteractions(projectRepository, taskRepository);
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
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));
        when(projectRepository.findByIdAndOwnerId(newProjectId, OWNER_ID))
                .thenReturn(Optional.of(newProject));

        var response = service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .projectId(newProjectId)
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
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
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
                        .build()
        );

        assertThat(response.getProjectId()).isEqualTo(newProjectId);
        assertThat(response.getTaskId()).isEqualTo(newTaskId);
    }

    @Test
    void clearsTaskWhenExplicitlyRequested() {
        TimeEntry entry = manualEntry(project, task);
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));

        var response = service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .clearTask(true)
                        .build()
        );

        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(response.getTaskId()).isNull();
    }

    @Test
    void updatesCompletedEntryTimeRangeAndRecalculatesDuration() {
        TimeEntry entry = manualEntry(project, task);
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));
        Instant newStart = NOW.minusSeconds(5 * 60);

        var response = service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .startedAt(newStart)
                        .endedAt(NOW)
                        .build()
        );

        assertThat(response.getStartedAt()).isEqualTo(newStart);
        assertThat(response.getEndedAt()).isEqualTo(NOW);
        assertThat(response.getDurationMinutes()).isEqualTo(5);
    }

    @Test
    void rejectsUpdateOfLockedEntry() {
        TimeEntry entry = manualEntry(project, task);
        entry.lock(NOW);
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .description("Must not change")
                        .build()
        )).isInstanceOf(TimeEntryLockedException.class);

        assertThat(entry.getDescription()).isEqualTo("Original work");
    }

    @Test
    void rejectsUpdateOfEntryNotOwnedByUser() {
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .description("Must not change")
                        .build()
        )).isInstanceOf(TimeEntryNotFoundException.class);
    }

    @Test
    void rejectsUpdateWithoutAnyFields() {
        assertThatThrownBy(() -> service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder().build()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one field must be provided");

        verify(timeEntryRepository, never())
                .findByIdAndOwnerId(any(), any());
    }

    @Test
    void deletesOwnedUnlockedCompletedEntry() {
        TimeEntry entry = manualEntry(project, task);
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));

        service.delete(OWNER_ID, ENTRY_ID);

        verify(timeEntryRepository).delete(entry);
    }

    @Test
    void rejectsDeleteOfLockedEntry() {
        TimeEntry entry = manualEntry(project, task);
        entry.lock(NOW);
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
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
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(runningTimer));

        assertThatThrownBy(() -> service.delete(OWNER_ID, ENTRY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("a running timer must be cancelled");

        verify(timeEntryRepository, never()).delete(any(TimeEntry.class));
    }

    @Test
    void rejectsDeleteOfEntryNotOwnedByUser() {
        when(timeEntryRepository.findByIdAndOwnerId(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(OWNER_ID, ENTRY_ID))
                .isInstanceOf(TimeEntryNotFoundException.class);

        verify(timeEntryRepository, never()).delete(any(TimeEntry.class));
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
