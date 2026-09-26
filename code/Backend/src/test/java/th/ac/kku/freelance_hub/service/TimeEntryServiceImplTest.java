package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.request.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.StartTimerRequest;
import th.ac.kku.freelance_hub.dto.request.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.exception.RunningTimerNotFoundException;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.exception.TimerAlreadyRunningException;
import th.ac.kku.freelance_hub.exception.TimeEntryLockedException;
import th.ac.kku.freelance_hub.exception.TimeEntryNotFoundException;
import th.ac.kku.freelance_hub.exception.UserNotFoundException;
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
                .email("timer-owner@example.com")
                .passwordHash("test-hash")
                .build();
        Client client = new Client(owner, "Timer Client");
        ReflectionTestUtils.setField(client, "id", UUID.randomUUID());

        project = new Project(owner, client, "Timer Project");
        project.changeStatus(ProjectStatus.ACTIVE);
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);

        task = new Task(project, "Timer Task", 0);
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
    void startsTimerForOwnedProjectAndTaskUsingServerClock() {
        stubOwnedUserAndProject();
        when(taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                TASK_ID, PROJECT_ID, OWNER_ID
        )).thenReturn(Optional.of(task));
        when(timeEntryRepository
                .existsByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(false);
        when(timeEntryRepository.saveAndFlush(any(TimeEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StartTimerRequest request = StartTimerRequest.builder()
                .projectId(PROJECT_ID)
                .taskId(TASK_ID)
                .description("  Build timer  ")
                .build();

        var response = service.startTimer(OWNER_ID, request);

        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(response.getTaskId()).isEqualTo(TASK_ID);
        assertThat(response.getDescription()).isEqualTo("Build timer");
        assertThat(response.getEntryType()).isEqualTo(EntryType.TIMER);
        assertThat(response.getStartedAt()).isEqualTo(NOW);
        assertThat(response.getEndedAt()).isNull();
        assertThat(response.isRunning()).isTrue();

        ArgumentCaptor<TimeEntry> captor = ArgumentCaptor.forClass(
                TimeEntry.class
        );
        verify(timeEntryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getOwner()).isSameAs(owner);
        assertThat(captor.getValue().getProject()).isSameAs(project);
        assertThat(captor.getValue().getTask()).isSameAs(task);
    }

    @Test
    void startsTimerWithoutOptionalTask() {
        stubOwnedUserAndProject();
        when(timeEntryRepository
                .existsByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(false);
        when(timeEntryRepository.saveAndFlush(any(TimeEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.startTimer(
                OWNER_ID,
                StartTimerRequest.builder()
                        .projectId(PROJECT_ID)
                        .build()
        );

        assertThat(response.getTaskId()).isNull();
        assertThat(response.isRunning()).isTrue();
        verifyNoInteractions(taskRepository);
    }

    @Test
    void rejectsMissingUser() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startTimer(
                OWNER_ID,
                StartTimerRequest.builder().projectId(PROJECT_ID).build()
        )).isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(projectRepository, taskRepository);
        verify(timeEntryRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsProjectNotOwnedByUser() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startTimer(
                OWNER_ID,
                StartTimerRequest.builder().projectId(PROJECT_ID).build()
        )).isInstanceOf(ProjectNotFoundException.class);

        verifyNoInteractions(taskRepository);
        verify(timeEntryRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsTaskOutsideOwnedProject() {
        stubOwnedUserAndProject();
        when(taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                TASK_ID, PROJECT_ID, OWNER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startTimer(
                OWNER_ID,
                StartTimerRequest.builder()
                        .projectId(PROJECT_ID)
                        .taskId(TASK_ID)
                        .build()
        )).isInstanceOf(TaskNotFoundException.class);

        verify(timeEntryRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsTimerForNonActiveProject() {
        Project plannedProject = new Project(
                owner,
                project.getClient(),
                "Planned Project"
        );
        ReflectionTestUtils.setField(plannedProject, "id", PROJECT_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(plannedProject));

        assertThatThrownBy(() -> service.startTimer(
                OWNER_ID,
                StartTimerRequest.builder().projectId(PROJECT_ID).build()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("project must be active to track time");

        verify(timeEntryRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsTimerWhenOneIsAlreadyRunning() {
        stubOwnedUserAndProject();
        when(timeEntryRepository
                .existsByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(true);

        assertThatThrownBy(() -> service.startTimer(
                OWNER_ID,
                StartTimerRequest.builder().projectId(PROJECT_ID).build()
        )).isInstanceOf(TimerAlreadyRunningException.class);

        verify(timeEntryRepository, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentInsertConflictIntoTimerAlreadyRunning() {
        stubOwnedUserAndProject();
        when(timeEntryRepository
                .existsByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(false);
        when(timeEntryRepository.saveAndFlush(any(TimeEntry.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "uk_time_entries_owner_running_timer"
                ));

        assertThatThrownBy(() -> service.startTimer(
                OWNER_ID,
                StartTimerRequest.builder().projectId(PROJECT_ID).build()
        )).isInstanceOf(TimerAlreadyRunningException.class);
    }

    @Test
    void preservesUnrelatedDatabaseIntegrityErrors() {
        stubOwnedUserAndProject();
        when(timeEntryRepository
                .existsByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(false);
        DataIntegrityViolationException databaseError =
                new DataIntegrityViolationException(
                        "unrelated foreign key constraint"
                );
        when(timeEntryRepository.saveAndFlush(any(TimeEntry.class)))
                .thenThrow(databaseError);

        assertThatThrownBy(() -> service.startTimer(
                OWNER_ID,
                StartTimerRequest.builder().projectId(PROJECT_ID).build()
        )).isSameAs(databaseError);
    }

    @Test
    void returnsCurrentRunningTimerWithoutLockingIt() {
        TimeEntry runningTimer = TimeEntry.startTimer(
                owner,
                project,
                task,
                "Current work",
                NOW
        );
        when(timeEntryRepository
                .findByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(Optional.of(runningTimer));

        var response = service.getCurrentTimer(OWNER_ID);

        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(response.getTaskId()).isEqualTo(TASK_ID);
        assertThat(response.getStartedAt()).isEqualTo(NOW);
        assertThat(response.isRunning()).isTrue();
        verify(timeEntryRepository, never())
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        any(), any()
                );
    }

    @Test
    void rejectsCurrentTimerRequestWhenNoneIsRunning() {
        when(timeEntryRepository
                .findByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentTimer(OWNER_ID))
                .isInstanceOf(RunningTimerNotFoundException.class);
    }

    @Test
    void stopsLockedRunningTimerUsingServerClock() {
        Instant startedAt = NOW.minusSeconds(90);
        TimeEntry runningTimer = TimeEntry.startTimer(
                owner,
                project,
                task,
                "Current work",
                startedAt
        );
        when(timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(Optional.of(runningTimer));

        var response = service.stopTimer(OWNER_ID);

        assertThat(response.getStartedAt()).isEqualTo(startedAt);
        assertThat(response.getEndedAt()).isEqualTo(NOW);
        assertThat(response.getDurationMinutes()).isEqualTo(2);
        assertThat(response.isRunning()).isFalse();
        assertThat(runningTimer.getEndedAt()).isEqualTo(NOW);
        assertThat(runningTimer.getDurationMinutes()).isEqualTo(2);
        verify(timeEntryRepository, never())
                .findByOwnerIdAndEntryTypeAndEndedAtIsNull(any(), any());
    }

    @Test
    void rejectsStopWhenNoTimerIsRunning() {
        when(timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.stopTimer(OWNER_ID))
                .isInstanceOf(RunningTimerNotFoundException.class);
    }

    @Test
    void cancelsLockedRunningTimer() {
        TimeEntry runningTimer = TimeEntry.startTimer(
                owner,
                project,
                null,
                "Discard this timer",
                NOW.minusSeconds(60)
        );
        when(timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(Optional.of(runningTimer));

        service.cancelTimer(OWNER_ID);

        verify(timeEntryRepository).delete(runningTimer);
        verify(timeEntryRepository, never())
                .findByOwnerIdAndEntryTypeAndEndedAtIsNull(any(), any());
    }

    @Test
    void rejectsCancelWhenNoTimerIsRunning() {
        when(timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelTimer(OWNER_ID))
                .isInstanceOf(RunningTimerNotFoundException.class);

        verify(timeEntryRepository, never()).delete(any(TimeEntry.class));
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

    @Test
    void listsEntriesWithCombinedFiltersPaginationAndSorting() {
        TimeEntry entry = manualEntry(project, task);
        PageRequest pageable = PageRequest.of(
                1,
                5,
                Sort.by(Sort.Direction.ASC, "durationMinutes")
        );
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any(),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(entry), pageable, 6));
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .clientId(project.getClient().getId())
                .projectId(PROJECT_ID)
                .taskId(TASK_ID)
                .entryType(EntryType.MANUAL)
                .from(NOW.minusSeconds(60 * 60))
                .to(NOW.plusSeconds(60 * 60))
                .page(1)
                .size(5)
                .sortBy("durationMinutes")
                .direction(Sort.Direction.ASC)
                .build();

        var result = service.list(OWNER_ID, filter);

        assertThat(result.getContent())
                .extracting(response -> response.getId())
                .containsExactly(ENTRY_ID);
        assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        verify(timeEntryRepository).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any(),
                eq(pageable)
        );
    }

    @Test
    void returnsEmptyPageWhenNoEntriesMatchListFilters() {
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "startedAt")
        );
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any(),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = service.list(
                OWNER_ID,
                TimeEntryFilterRequest.builder().build()
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void rejectsListWithInvalidPageOrSize() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .page(-1)
                .build();

        assertThatThrownBy(() -> service.list(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Time entry page size must be between 1 and 100");

        verify(timeEntryRepository, never()).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any(),
                any(PageRequest.class)
        );
    }

    @Test
    void rejectsListWithUnsupportedSortField() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .sortBy("owner")
                .build();

        assertThatThrownBy(() -> service.list(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported time entry sort field: owner");
    }

    @Test
    void rejectsListWithoutSortDirection() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .direction(null)
                .build();

        assertThatThrownBy(() -> service.list(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Time entry sort direction is required");
    }

    @Test
    void rejectsListWithInvalidTimeRange() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .from(NOW)
                .to(NOW)
                .build();

        assertThatThrownBy(() -> service.list(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("From time must be before to time");
    }

    @Test
    void summarizesCompletedEntriesAndExcludesRunningTimer() {
        Instant from = NOW.minusSeconds(24 * 60 * 60);
        Instant to = NOW.plusSeconds(1);
        TimeEntry oneMinuteEntry = manualEntry(project, task);
        TimeEntry thirtyMinuteEntry = TimeEntry.createManualWithDuration(
                owner,
                project,
                null,
                "Long task",
                NOW.minusSeconds(60 * 60),
                30
        );
        TimeEntry runningTimer = TimeEntry.startTimer(
                owner,
                project,
                null,
                "Still running",
                NOW.minusSeconds(5 * 60)
        );
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        )).thenReturn(List.of(
                oneMinuteEntry,
                thirtyMinuteEntry,
                runningTimer
        ));
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .projectId(PROJECT_ID)
                .from(from)
                .to(to)
                .build();

        var response = service.summarize(OWNER_ID, filter);

        assertThat(response.getFrom()).isEqualTo(from);
        assertThat(response.getTo()).isEqualTo(to);
        assertThat(response.getEntryCount()).isEqualTo(2);
        assertThat(response.getTotalMinutes()).isEqualTo(31);
        verify(timeEntryRepository).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        );
    }

    @Test
    void returnsZeroSummaryWhenNoCompletedEntriesMatch() {
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        )).thenReturn(List.of());

        var response = service.summarize(
                OWNER_ID,
                TimeEntryFilterRequest.builder().build()
        );

        assertThat(response.getEntryCount()).isZero();
        assertThat(response.getTotalMinutes()).isZero();
        assertThat(response.getFrom()).isNull();
        assertThat(response.getTo()).isNull();
    }

    @Test
    void summaryIgnoresPaginationAndSortingOptions() {
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        )).thenReturn(List.of());
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .page(-1)
                .size(0)
                .sortBy(null)
                .direction(null)
                .build();

        var response = service.summarize(OWNER_ID, filter);

        assertThat(response.getEntryCount()).isZero();
        verify(timeEntryRepository).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        );
        verify(timeEntryRepository, never()).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any(),
                any(PageRequest.class)
        );
    }

    @Test
    void rejectsSummaryWithInvalidTimeRange() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .from(NOW)
                .to(NOW)
                .build();

        assertThatThrownBy(() -> service.summarize(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("From time must be before to time");

        verify(timeEntryRepository, never()).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        );
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
