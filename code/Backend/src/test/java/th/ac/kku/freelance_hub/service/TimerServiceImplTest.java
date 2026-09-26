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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.request.StartTimerRequest;
import th.ac.kku.freelance_hub.event.TimerStoppedEvent;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.exception.RunningTimerNotFoundException;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.exception.TimerAlreadyRunningException;
import th.ac.kku.freelance_hub.exception.UserNotFoundException;
import th.ac.kku.freelance_hub.mapper.TimeEntryMapper;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.impl.TimerServiceImpl;

@ExtendWith(MockitoExtension.class)
class TimerServiceImplTest {

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
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TimerServiceImpl timerService;
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

        timerService = new TimerServiceImpl(
                timeEntryRepository,
                projectRepository,
                taskRepository,
                userRepository,
                new TimeEntryMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                eventPublisher
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

        var response = timerService.startTimer(OWNER_ID, request);

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

        var response = timerService.startTimer(
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

        assertThatThrownBy(() -> timerService.startTimer(
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

        assertThatThrownBy(() -> timerService.startTimer(
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

        assertThatThrownBy(() -> timerService.startTimer(
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

        assertThatThrownBy(() -> timerService.startTimer(
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

        assertThatThrownBy(() -> timerService.startTimer(
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

        assertThatThrownBy(() -> timerService.startTimer(
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

        assertThatThrownBy(() -> timerService.startTimer(
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

        var response = timerService.getCurrentTimer(OWNER_ID);

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

        assertThatThrownBy(() -> timerService.getCurrentTimer(OWNER_ID))
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
        ReflectionTestUtils.setField(runningTimer, "id", ENTRY_ID);
        when(timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(Optional.of(runningTimer));

        var response = timerService.stopTimer(OWNER_ID);

        assertThat(response.getStartedAt()).isEqualTo(startedAt);
        assertThat(response.getEndedAt()).isEqualTo(NOW);
        assertThat(response.getDurationMinutes()).isEqualTo(2);
        assertThat(response.isRunning()).isFalse();
        assertThat(runningTimer.getEndedAt()).isEqualTo(NOW);
        assertThat(runningTimer.getDurationMinutes()).isEqualTo(2);
        ArgumentCaptor<TimerStoppedEvent> eventCaptor =
                ArgumentCaptor.forClass(TimerStoppedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TimerStoppedEvent event = eventCaptor.getValue();
        assertThat(event.timeEntryId()).isEqualTo(ENTRY_ID);
        assertThat(event.ownerId()).isEqualTo(OWNER_ID);
        assertThat(event.projectId()).isEqualTo(PROJECT_ID);
        assertThat(event.taskId()).isEqualTo(TASK_ID);
        assertThat(event.durationMinutes()).isEqualTo(2);
        assertThat(event.startedAt()).isEqualTo(startedAt);
        assertThat(event.endedAt()).isEqualTo(NOW);
        verify(timeEntryRepository, never())
                .findByOwnerIdAndEntryTypeAndEndedAtIsNull(any(), any());
    }

    @Test
    void rejectsStopWhenNoTimerIsRunning() {
        when(timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        OWNER_ID, EntryType.TIMER
                )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timerService.stopTimer(OWNER_ID))
                .isInstanceOf(RunningTimerNotFoundException.class);

        verify(eventPublisher, never()).publishEvent(
                any(TimerStoppedEvent.class)
        );
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

        timerService.cancelTimer(OWNER_ID);

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

        assertThatThrownBy(() -> timerService.cancelTimer(OWNER_ID))
                .isInstanceOf(RunningTimerNotFoundException.class);

        verify(timeEntryRepository, never()).delete(any(TimeEntry.class));
    }


    private void stubOwnedUserAndProject() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
    }
}

