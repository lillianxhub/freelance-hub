package th.ac.kku.freelance_hub.service;

import th.ac.kku.freelance_hub.exception.InvalidStateException;
import th.ac.kku.freelance_hub.exception.InvalidArgumentException;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.dto.request.timeentry.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.UpdateTimeEntryRequest;
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
        assertThat(response.getTask().status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.isRunning()).isFalse();
        verify(timeEntryRepository).save(any(TimeEntry.class));
        verify(timeEntryRepository, never())
                .saveAndFlush(any(TimeEntry.class));
        verify(taskRepository).saveAndFlush(task);
    }

    @Test
    void leavesInProgressTaskUnchangedWhenCreatingManualEntry() {
        task.start();
        stubOwnedUserAndProject();
        when(taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                TASK_ID, PROJECT_ID, OWNER_ID
        )).thenReturn(Optional.of(task));
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createManual(
                OWNER_ID,
                ManualTimeEntryRequest.builder()
                        .projectId(PROJECT_ID)
                        .taskId(TASK_ID)
                        .startedAt(NOW)
                        .durationSeconds(90L)
                        .build()
        );

        assertThat(response.getTask().status()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(timeEntryRepository).save(any(TimeEntry.class));
        verify(taskRepository, never()).saveAndFlush(any(Task.class));
    }

    @Test
    void rejectsCompletedTaskBeforeSavingManualEntry() {
        task.complete(NOW.minusSeconds(60));
        stubOwnedUserAndProject();
        when(taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                TASK_ID, PROJECT_ID, OWNER_ID
        )).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.createManual(
                OWNER_ID,
                ManualTimeEntryRequest.builder()
                        .projectId(PROJECT_ID)
                        .taskId(TASK_ID)
                        .startedAt(NOW)
                        .durationSeconds(90L)
                        .build()
        )).isInstanceOf(InvalidStateException.class)
                .hasMessage("ไม่สามารถเริ่มงานที่เสร็จสิ้นแล้ว");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        verify(timeEntryRepository, never()).save(any(TimeEntry.class));
        verify(taskRepository, never()).saveAndFlush(any(Task.class));
    }

    @Test
    void rejectsManualEntryForPlannedProjectBeforeSavingOrStartingTask() {
        Project plannedProject = new Project(
                owner,
                project.getClient(),
                "Planned Project"
        );
        ReflectionTestUtils.setField(plannedProject, "id", PROJECT_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(plannedProject));
        when(taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                TASK_ID, PROJECT_ID, OWNER_ID
        )).thenReturn(Optional.of(new Task(plannedProject, "Planned Task", 0)));

        assertThatThrownBy(() -> service.createManual(
                OWNER_ID,
                ManualTimeEntryRequest.builder()
                        .projectId(PROJECT_ID)
                        .taskId(TASK_ID)
                        .description("Planned work")
                        .startedAt(NOW)
                        .durationSeconds(90L)
                        .build()
        )).isInstanceOf(InvalidStateException.class)
                .hasMessage("โปรเจกต์ต้องอยู่ในสถานะกำลังทำก่อนบันทึกเวลา");

        verify(timeEntryRepository, never()).save(any(TimeEntry.class));
        verify(taskRepository, never()).saveAndFlush(any(Task.class));
    }

    @Test
    void rejectsManualEntryWithoutEndTimeOrDuration() {
        ManualTimeEntryRequest request = ManualTimeEntryRequest.builder()
                .projectId(PROJECT_ID)
                .startedAt(NOW)
                .build();

        assertThatThrownBy(() -> service.createManual(OWNER_ID, request))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage(
                        "กรุณาระบุเวลาสิ้นสุดหรือระยะเวลาอย่างใดอย่างหนึ่ง"
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
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage(
                        "กรุณาระบุเวลาสิ้นสุดหรือระยะเวลาอย่างใดอย่างหนึ่ง"
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
        newProject.changeStatus(ProjectStatus.ACTIVE);
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
        newProject.changeStatus(ProjectStatus.ACTIVE);
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
    void rejectsUpdateToPlannedProjectWithoutChangingEntry() {
        TimeEntry entry = manualEntry(project, task);
        Instant originalStart = entry.getStartedAt();
        UUID plannedProjectId = UUID.randomUUID();
        Project plannedProject = new Project(owner, project.getClient(), "Planned Project");
        ReflectionTestUtils.setField(plannedProject, "id", plannedProjectId);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));
        when(projectRepository.findByIdAndOwnerId(plannedProjectId, OWNER_ID))
                .thenReturn(Optional.of(plannedProject));

        assertThatThrownBy(() -> service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .projectId(plannedProjectId)
                        .startedAt(NOW)
                        .durationSeconds(90L)
                        .build()
        )).isInstanceOf(InvalidStateException.class)
                .hasMessage("โปรเจกต์ต้องอยู่ในสถานะกำลังทำก่อนบันทึกเวลา");

        assertThat(entry.getProject()).isSameAs(project);
        assertThat(entry.getStartedAt()).isEqualTo(originalStart);
        verify(timeEntryRepository, never()).flush();
        verifyNoInteractions(taskRepository);
    }

    @Test
    void rejectsUpdatingEntryWhoseCurrentProjectIsOnHold() {
        TimeEntry entry = manualEntry(project, task);
        project.changeStatus(ProjectStatus.ON_HOLD);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.update(
                OWNER_ID,
                ENTRY_ID,
                UpdateTimeEntryRequest.builder()
                        .projectId(UUID.randomUUID())
                        .startedAt(NOW)
                        .durationSeconds(90L)
                        .build()
        )).isInstanceOf(InvalidStateException.class)
                .hasMessage("โปรเจกต์ต้องอยู่ในสถานะกำลังทำก่อนบันทึกเวลา");

        verifyNoInteractions(projectRepository, taskRepository);
        verify(timeEntryRepository, never()).flush();
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
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("กรุณาระบุโปรเจกต์");

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
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("กรุณายกเลิกตัวจับเวลาก่อนลบรายการเวลา");

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
                .isInstanceOf(InvalidStateException.class);
        assertThatThrownBy(() -> manual.softDelete(NOW))
                .isInstanceOf(InvalidStateException.class);
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
                .isInstanceOf(InvalidStateException.class);

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

    @Nested
    class Queries {

        private static final UUID OWNER_ID = UUID.randomUUID();
        private static final UUID PROJECT_ID = UUID.randomUUID();
        private static final UUID TASK_ID = UUID.randomUUID();
        private static final UUID ENTRY_ID = UUID.randomUUID();
        private static final Instant NOW = Instant.parse("2026-09-26T08:30:00Z");

        @Mock
        private TimeEntryRepository timeEntryRepository;
        @Mock
        private ProjectRepository projectRepository;

        private TimeEntryServiceImpl queryService;
        private User owner;
        private Project project;
        private Task task;

        @BeforeEach
        void setUp() {
            owner = User.builder()
                    .id(OWNER_ID)
                    .email("query-owner@example.com")
                    .passwordHash("test-hash")
                    .build();
            Client client = new Client(owner, "Query Client");
            ReflectionTestUtils.setField(client, "id", UUID.randomUUID());
            project = new Project(owner, client, "Query Project");
            project.changeStatus(ProjectStatus.ACTIVE);
            ReflectionTestUtils.setField(project, "id", PROJECT_ID);
            task = new Task(project, "Query Task", 0);
            ReflectionTestUtils.setField(task, "id", TASK_ID);

            queryService = new TimeEntryServiceImpl(
                    timeEntryRepository,
                    projectRepository,
                    taskRepository,
                    userRepository,
                    new TimeEntryMapper(),
                    Clock.fixed(NOW, ZoneOffset.UTC)
            );
        }

        @Test
        void getsOwnedEntryById() {
            TimeEntry entry = manualEntry(project, task);
            when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                    .thenReturn(Optional.of(entry));

            var response = queryService.getById(OWNER_ID, ENTRY_ID);

            assertThat(response.getId()).isEqualTo(ENTRY_ID);
            assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(response.getTaskId()).isEqualTo(TASK_ID);
            verify(timeEntryRepository)
                    .findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID);
        }

        @Test
        void rejectsMissingOrOtherOwnersEntryById() {
            when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryService.getById(OWNER_ID, ENTRY_ID))
                    .isInstanceOf(TimeEntryNotFoundException.class);

            verify(timeEntryRepository)
                    .findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID);
        }

        @Test
        void listsEntriesWithCombinedFiltersPaginationAndSorting() {
            TimeEntry entry = manualEntry(project, task);
            PageRequest pageable = PageRequest.of(
                    1,
                    5,
                    Sort.by(Sort.Direction.ASC, "durationSeconds")
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
                    .page(2)
                    .limit(5)
                    .sortBy("durationSeconds")
                    .direction(Sort.Direction.ASC)
                    .build();

            var result = queryService.list(OWNER_ID, filter);

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

            var result = queryService.list(
                    OWNER_ID,
                    TimeEntryFilterRequest.builder().build()
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void rejectsListWithInvalidPageOrLimit() {
            TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                    .page(0)
                    .build();

            assertThatThrownBy(() -> queryService.list(OWNER_ID, filter))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessage(
                            "หมายเลขหน้าต้องเริ่มจาก 1 และจำนวนรายการต่อหน้าต้องอยู่ระหว่าง 1 ถึง 100"
                    );

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

            assertThatThrownBy(() -> queryService.list(OWNER_ID, filter))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessage("ฟิลด์ที่ใช้เรียงลำดับไม่ถูกต้อง");
        }

        @Test
        void rejectsListWithoutSortDirection() {
            TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                    .direction(null)
                    .build();

            assertThatThrownBy(() -> queryService.list(OWNER_ID, filter))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessage("กรุณาระบุทิศทางการเรียงรายการเวลา");
        }

        @Test
        void rejectsListWithInvalidTimeRange() {
            TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                    .from(NOW)
                    .to(NOW)
                    .build();

            assertThatThrownBy(() -> queryService.list(OWNER_ID, filter))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessage("เวลาเริ่มต้นต้องอยู่ก่อนเวลาสิ้นสุด");
        }

        @Test
        void summarizesCompletedEntriesAndExcludesRunningTimer() {
            Instant from = NOW.minusSeconds(24 * 60 * 60);
            Instant to = NOW.plusSeconds(1);
            TimeEntry oneMinuteEntry = manualEntry(project, task);
            TimeEntry thirtyMinuteEntry = TimeEntry.createManualWithDurationSeconds(
                    owner,
                    project,
                    null,
                    "Long task",
                    NOW.minusSeconds(60 * 60),
                    1800
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

            var response = queryService.summarize(OWNER_ID, filter);

            assertThat(response.getFrom()).isEqualTo(from);
            assertThat(response.getTo()).isEqualTo(to);
            assertThat(response.getEntryCount()).isEqualTo(2);
            assertThat(response.getTotalSeconds()).isEqualTo(1860);
            verify(timeEntryRepository).findAll(
                    ArgumentMatchers.<Specification<TimeEntry>>any()
            );
        }

        @Test
        void returnsZeroSummaryWhenNoCompletedEntriesMatch() {
            when(timeEntryRepository.findAll(
                    ArgumentMatchers.<Specification<TimeEntry>>any()
            )).thenReturn(List.of());

            var response = queryService.summarize(
                    OWNER_ID,
                    TimeEntryFilterRequest.builder().build()
            );

            assertThat(response.getEntryCount()).isZero();
            assertThat(response.getTotalSeconds()).isZero();
            assertThat(response.getFrom()).isNull();
            assertThat(response.getTo()).isNull();
        }

        @Test
        void summaryIgnoresPaginationAndSortingOptions() {
            when(timeEntryRepository.findAll(
                    ArgumentMatchers.<Specification<TimeEntry>>any()
            )).thenReturn(List.of());
            TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                    .page(0)
                    .limit(0)
                    .sortBy(null)
                    .direction(null)
                    .build();

            var response = queryService.summarize(OWNER_ID, filter);

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

            assertThatThrownBy(() -> queryService.summarize(OWNER_ID, filter))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessage("เวลาเริ่มต้นต้องอยู่ก่อนเวลาสิ้นสุด");

            verify(timeEntryRepository, never()).findAll(
                    ArgumentMatchers.<Specification<TimeEntry>>any()
            );
        }

        @Test
        void convertsThaiDateBoundariesAndReturnsTotalSeconds() {
            LocalDate day = LocalDate.of(2026, 9, 29);
            Instant from = Instant.parse("2026-09-28T17:00:00Z");
            Instant to = Instant.parse("2026-09-29T17:00:00Z");
            when(timeEntryRepository.findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                    OWNER_ID, from, to,
                    TimeEntryRepository.AllocationView.class
            )).thenReturn(List.of(durationEntry(from, 1800), durationEntry(from, 1800)));

            long total = queryService.sumCompletedSeconds(
                    OWNER_ID, day, day.plusDays(1)
            );

            assertThat(total).isEqualTo(3600L);
            verify(timeEntryRepository).findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                    OWNER_ID, from, to,
                    TimeEntryRepository.AllocationView.class
            );
        }

        @Test
        void groupsByThaiStartDateAndIncludesEmptyDays() {
            LocalDate fromDay = LocalDate.of(2026, 9, 29);
            LocalDate toDay = fromDay.plusDays(3);
            Instant from = Instant.parse("2026-09-28T17:00:00Z");
            Instant to = Instant.parse("2026-10-01T17:00:00Z");
            when(timeEntryRepository.findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                    OWNER_ID, from, to,
                    TimeEntryRepository.AllocationView.class
            )).thenReturn(List.of(
                    durationEntry(Instant.parse("2026-09-28T17:00:00Z"), 1800),
                    durationEntry(Instant.parse("2026-09-29T16:59:00Z"), 900),
                    durationEntry(Instant.parse("2026-09-30T17:00:00Z"), 600)
            ));

            var result = queryService.sumDailySeconds(OWNER_ID, fromDay, toDay);

            assertThat(result).containsExactly(
                    new TimeEntryService.DailySeconds(fromDay, 2700),
                    new TimeEntryService.DailySeconds(fromDay.plusDays(1), 0),
                    new TimeEntryService.DailySeconds(fromDay.plusDays(2), 600)
            );
        }

        @Test
        void returnsZeroForEveryDayWhenThereAreNoEntries() {
            LocalDate fromDay = LocalDate.of(2026, 9, 29);
            Instant from = Instant.parse("2026-09-28T17:00:00Z");
            Instant to = Instant.parse("2026-09-30T17:00:00Z");
            when(timeEntryRepository.findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                    OWNER_ID, from, to,
                    TimeEntryRepository.AllocationView.class
            )).thenReturn(List.of());

            var result = queryService.sumDailySeconds(
                    OWNER_ID, fromDay, fromDay.plusDays(2)
            );

            assertThat(result).containsExactly(
                    new TimeEntryService.DailySeconds(fromDay, 0),
                    new TimeEntryService.DailySeconds(fromDay.plusDays(1), 0)
            );
        }

        @Test
        void sumsByProjectAndIncludesProjectWithNoEntries() {
            LocalDate day = LocalDate.of(2026, 9, 29);
            Instant from = Instant.parse("2026-09-28T17:00:00Z");
            Instant to = Instant.parse("2026-09-29T17:00:00Z");
            Project empty = new Project(owner, project.getClient(), "Empty Project");
            ReflectionTestUtils.setField(empty, "id", UUID.randomUUID());
            when(projectRepository.findAll(
                    ArgumentMatchers.<Specification<Project>>any(),
                    eq(Sort.by("name", "id"))
            )).thenReturn(List.of(empty, project));
            when(timeEntryRepository.findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                    OWNER_ID, from, to, TimeEntryRepository.AllocationView.class
            )).thenReturn(List.of(
                    durationEntry(from, 90),
                    durationEntry(from, 30, false, true),
                    durationEntry(from, 40, true, false)
            ));

            assertThat(queryService.sumSecondsByProject(OWNER_ID, day, day.plusDays(1)))
                    .containsExactly(
                            new TimeEntryService.ProjectSeconds(empty.getId(), empty.getName(), 0),
                            new TimeEntryService.ProjectSeconds(PROJECT_ID, project.getName(), 90)
                    );
        }

        @Test
        void excludesInactiveProjectsAndDeletedTasksFromTotalAndDaily() {
            LocalDate day = LocalDate.of(2026, 9, 29);
            Instant from = Instant.parse("2026-09-28T17:00:00Z");
            Instant to = Instant.parse("2026-09-29T17:00:00Z");
            when(timeEntryRepository.findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                    OWNER_ID, from, to, TimeEntryRepository.AllocationView.class
            )).thenReturn(List.of(
                    durationEntry(from, 90),
                    durationEntry(from, 30, false, true),
                    durationEntry(from, 40, true, false)
            ));

            assertThat(queryService.sumCompletedSeconds(OWNER_ID, day, day.plusDays(1)))
                    .isEqualTo(90);
            assertThat(queryService.sumDailySeconds(OWNER_ID, day, day.plusDays(1)))
                    .containsExactly(new TimeEntryService.DailySeconds(day, 90));
        }

        @Test
        void rejectsEmptyOrReversedAnalyticsDateRange() {
            LocalDate day = LocalDate.of(2026, 9, 29);

            assertThatThrownBy(() -> queryService.sumCompletedSeconds(
                    OWNER_ID, day, day
            )).isInstanceOf(InvalidArgumentException.class);
            assertThatThrownBy(() -> queryService.sumCompletedSeconds(
                    OWNER_ID, day, day.minusDays(1)
            )).isInstanceOf(InvalidArgumentException.class);
            assertThatThrownBy(() -> queryService.sumDailySeconds(
                    OWNER_ID, day, day
            )).isInstanceOf(InvalidArgumentException.class);
            verifyNoInteractions(timeEntryRepository);
        }

        private static TimeEntryRepository.AllocationView durationEntry(
                Instant startedAt,
                long durationSeconds
        ) {
            return durationEntry(startedAt, durationSeconds, true, true);
        }

        private static TimeEntryRepository.AllocationView durationEntry(
                Instant startedAt, long durationSeconds,
                boolean projectActive, boolean taskActive
        ) {
            return new TimeEntryRepository.AllocationView() {
                @Override
                public Instant getStartedAt() {
                    return startedAt;
                }

                @Override
                public Long getDurationSeconds() {
                    return durationSeconds;
                }

                @Override
                public TimeEntryRepository.ProjectView getProject() {
                    return new TimeEntryRepository.ProjectView() {
                        public UUID getId() { return PROJECT_ID; }
                        public Boolean getIsActive() { return projectActive; }
                    };
                }

                @Override
                public TimeEntryRepository.TaskView getTask() {
                    return new TimeEntryRepository.TaskView() {
                        public Boolean getIsActive() { return taskActive; }
                    };
                }
            };
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
}
