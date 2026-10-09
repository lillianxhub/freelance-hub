package th.ac.kku.freelance_hub.service;

import th.ac.kku.freelance_hub.exception.InvalidStateException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.exception.ClientNotFoundException;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.mapper.ProjectMapper;
import th.ac.kku.freelance_hub.mapper.TaskMapper;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.impl.ProjectServiceImpl;

import th.ac.kku.freelance_hub.repository.TaskRepository;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.dto.request.project.ChangeProjectStatusRequest;
import th.ac.kku.freelance_hub.dto.request.project.CreateProjectRequest;
import th.ac.kku.freelance_hub.dto.request.project.UpdateProjectRequest;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntrySummaryResponse;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryResponse;
@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TimeEntryService timeEntryService;

    @Mock
    private TimerService timerService;

    private ProjectServiceImpl service;
    private User owner;
    private Client client;

    @BeforeEach
    void setUp() {
        service = new ProjectServiceImpl(
                projectRepository,
                clientRepository,
                userRepository,
                new ProjectMapper(),
                taskRepository,
                new TaskMapper(),
                timeEntryService,
                timerService
        );

        owner = User.builder().id(OWNER_ID).build();
        client = new Client(owner, "Acme");
        ReflectionTestUtils.setField(client, "id", CLIENT_ID);
    }

    @Test
    void createsProjectOnlyWithOwnedClient() {
        when(userRepository.findById(OWNER_ID))
                .thenReturn(Optional.of(owner));
        when(clientRepository.findByIdAndOwnerId(CLIENT_ID, OWNER_ID))
                .thenReturn(Optional.of(client));
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateProjectRequest request = CreateProjectRequest.builder()
                .clientId(CLIENT_ID)
                .name("Website")
                .targetMinutes(120)
                .build();

        var response = service.create(OWNER_ID, request);

        assertThat(response.getName()).isEqualTo("Website");
        assertThat(response.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.PLANNED);

        verify(projectRepository).save(argThat(project ->
                project.getOwner() == owner
                        && project.getClient() == client
                        && project.getTargetMinutes() == 120
        ));
    }

    @Test
    void rejectsClientNotOwnedByUser() {
        when(userRepository.findById(OWNER_ID))
                .thenReturn(Optional.of(owner));
        when(clientRepository.findByIdAndOwnerId(CLIENT_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        CreateProjectRequest request = CreateProjectRequest.builder()
                .clientId(CLIENT_ID)
                .name("Website")
                .build();

        assertThatThrownBy(() -> service.create(OWNER_ID, request))
                .isInstanceOf(ClientNotFoundException.class);

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void doesNotReturnProjectOwnedByAnotherUser() {
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(OWNER_ID, PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(projectRepository)
                .findByIdAndOwnerId(PROJECT_ID, OWNER_ID);
    }

    @Test
    void countsActiveAndCompletedProjectsForOwner() {
        when(projectRepository.countByOwnerIdAndStatusAndDeletedAtIsNull(
                OWNER_ID, ProjectStatus.ACTIVE
        )).thenReturn(3L);
        when(projectRepository.countByOwnerIdAndStatusAndDeletedAtIsNull(
                OWNER_ID, ProjectStatus.COMPLETED
        )).thenReturn(2L);

        var counts = service.countActiveAndCompleted(OWNER_ID);

        assertThat(counts.activeCount()).isEqualTo(3);
        assertThat(counts.completedCount()).isEqualTo(2);
        assertThat(counts.totalCount()).isEqualTo(5);
    }

    @ParameterizedTest
    @CsvSource({
            "0,0.00,BELOW_80",
            "4799,79.98,BELOW_80",
            "4800,80.00,REACHED_80",
            "6000,100.00,REACHED_100",
            "6600,110.00,REACHED_100"
    })
    void calculatesProgressFromCompletedTimeEntrySeconds(
            long trackedSeconds,
            String expectedPercent,
            ProjectService.ProgressLevel expectedLevel
    ) {
        Project project = new Project(owner, client, "Website");
        project.updateDetails("Website", null, null, null, null, 100);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(timeEntryService.summarize(
                eq(OWNER_ID),
                argThat(filter -> PROJECT_ID.equals(filter.getProjectId()))
        )).thenReturn(TimeEntrySummaryResponse.builder()
                .totalSeconds(trackedSeconds)
                .build());

        var progress = service.getProgress(OWNER_ID, PROJECT_ID);

        assertThat(progress.targetMinutes()).isEqualTo(100);
        assertThat(progress.trackedSeconds()).isEqualTo(trackedSeconds);
        assertThat(progress.progressPercent())
                .isEqualByComparingTo(expectedPercent);
        assertThat(progress.level()).isEqualTo(expectedLevel);
    }

    @Test
    void progressWithoutTargetKeepsTrackedTimeWithoutPercent() {
        Project project = new Project(owner, client, "No target");
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(timeEntryService.summarize(
                eq(OWNER_ID),
                argThat(filter -> PROJECT_ID.equals(filter.getProjectId()))
        )).thenReturn(TimeEntrySummaryResponse.builder()
                .totalSeconds(3600)
                .build());

        var progress = service.getProgress(OWNER_ID, PROJECT_ID);

        assertThat(progress.trackedSeconds()).isEqualTo(3600);
        assertThat(progress.targetMinutes()).isNull();
        assertThat(progress.progressPercent()).isNull();
        assertThat(progress.level())
                .isEqualTo(ProjectService.ProgressLevel.NO_TARGET);
    }

    @Test
    void progressDoesNotReadTimeForUnknownProject() {
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgress(OWNER_ID, PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class);
        verifyNoInteractions(timeEntryService);
    }

    @ParameterizedTest
    @EnumSource(ProjectStatus.class)
    void rejectsStatusChangeWhileSameProjectTimerRuns(ProjectStatus nextStatus) {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.changeStatus(ProjectStatus.ACTIVE);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(timerService.getCurrentTimer(OWNER_ID))
                .thenReturn(Optional.of(TimeEntryResponse.builder()
                        .projectId(PROJECT_ID)
                        .build()));

        assertThatThrownBy(() -> service.changeStatus(
                OWNER_ID,
                PROJECT_ID,
                ChangeProjectStatusRequest.builder().status(nextStatus).build()
        )).isInstanceOf(InvalidStateException.class)
                .hasMessage("กรุณาหยุดจับเวลาก่อนเปลี่ยนสถานะโปรเจกต์");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void allowsStatusChangeWhenTimerRunsInAnotherProject() {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.changeStatus(ProjectStatus.ACTIVE);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(timerService.getCurrentTimer(OWNER_ID))
                .thenReturn(Optional.of(TimeEntryResponse.builder()
                        .projectId(UUID.randomUUID())
                        .build()));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service.changeStatus(
                OWNER_ID,
                PROJECT_ID,
                ChangeProjectStatusRequest.builder()
                        .status(ProjectStatus.ON_HOLD)
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.ON_HOLD);
        verify(projectRepository).save(project);
        verifyNoInteractions(timeEntryService);
    }

    @Test
    void rejectsCompletionWhenActiveTasksRemainIncomplete() {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.changeStatus(ProjectStatus.ACTIVE);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        TaskRepository.TaskProgressSummary summary =
                mock(TaskRepository.TaskProgressSummary.class);
        when(summary.getTotalTasks()).thenReturn(2L);
        when(summary.getCompletedTasks()).thenReturn(1L);
        when(taskRepository.summarizeProgressByProjectIds(
                OWNER_ID, List.of(PROJECT_ID), TaskStatus.COMPLETED))
                .thenReturn(List.of(summary));

        assertThatThrownBy(() -> service.changeStatus(
                OWNER_ID,
                PROJECT_ID,
                ChangeProjectStatusRequest.builder()
                        .status(ProjectStatus.COMPLETED)
                        .build()
        )).isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("ยังมีงานย่อยที่ไม่เสร็จ");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        verify(projectRepository, never()).save(any(Project.class));
        verifyNoInteractions(timeEntryService);
    }

    @Test
    void locksTimeEntriesWhenProjectBecomesCompleted() {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.changeStatus(ProjectStatus.ACTIVE);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        TaskRepository.TaskProgressSummary summary =
                mock(TaskRepository.TaskProgressSummary.class);
        when(summary.getTotalTasks()).thenReturn(2L);
        when(summary.getCompletedTasks()).thenReturn(2L);
        when(taskRepository.summarizeProgressByProjectIds(
                OWNER_ID, List.of(PROJECT_ID), TaskStatus.COMPLETED))
                .thenReturn(List.of(summary));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service.changeStatus(
                OWNER_ID,
                PROJECT_ID,
                ChangeProjectStatusRequest.builder()
                        .status(ProjectStatus.COMPLETED)
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verify(timeEntryService).lockByProject(OWNER_ID, PROJECT_ID);
        verify(projectRepository).save(project);
    }

    @Test
    void allowsCompletionWhenProjectHasNoActiveTasks() {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.changeStatus(ProjectStatus.ACTIVE);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service.changeStatus(
                OWNER_ID,
                PROJECT_ID,
                ChangeProjectStatusRequest.builder()
                        .status(ProjectStatus.COMPLETED)
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verify(taskRepository).summarizeProgressByProjectIds(
                OWNER_ID, List.of(PROJECT_ID), TaskStatus.COMPLETED);
        verify(timeEntryService).lockByProject(OWNER_ID, PROJECT_ID);
    }

    @Test
    void doesNotRelockTimeEntriesWhenProjectIsAlreadyCompleted() {
        Project project = new Project(owner, client, "Website");
        project.changeStatus(ProjectStatus.ACTIVE);
        project.changeStatus(ProjectStatus.COMPLETED);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service.changeStatus(
                OWNER_ID,
                PROJECT_ID,
                ChangeProjectStatusRequest.builder()
                        .status(ProjectStatus.COMPLETED)
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verifyNoInteractions(timeEntryService);
    }

    @Test
    void rejectsArchiveWhileSameProjectTimerRuns() {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.changeStatus(ProjectStatus.ACTIVE);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(timerService.getCurrentTimer(OWNER_ID))
                .thenReturn(Optional.of(TimeEntryResponse.builder()
                        .projectId(PROJECT_ID)
                        .build()));

        assertThatThrownBy(() -> service.archive(OWNER_ID, PROJECT_ID))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("กรุณาหยุดจับเวลาก่อนเปลี่ยนสถานะโปรเจกต์");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(project.getIsActive()).isTrue();
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void rejectsInvalidStatusTransitionWithoutSaving() {
        Project project = new Project(owner, client, "Website");
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));

        ChangeProjectStatusRequest request =
                ChangeProjectStatusRequest.builder()
                        .status(ProjectStatus.COMPLETED)
                        .build();

        assertThatThrownBy(() ->
                service.changeStatus(OWNER_ID, PROJECT_ID, request)
        ).isInstanceOf(InvalidStateException.class);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void listsProjectsWithClientTargetHoursAndMatchingTaskProgress() {
        Project projectWithoutTasks = new Project(
                owner,
                client,
                "No tasks"
        );
        ReflectionTestUtils.setField(
                projectWithoutTasks,
                "id",
                PROJECT_ID
        );

        UUID secondProjectId = UUID.randomUUID();

        Project projectWithTasks = new Project(
                owner,
                client,
                "Website"
        );
        projectWithTasks.updateDetails(
                "Website",
                null,
                null,
                null,
                null,
                120
        );
        ReflectionTestUtils.setField(
                projectWithTasks,
                "id",
                secondProjectId
        );

        PageRequest pageable = PageRequest.of(
                1,
                5,
                Sort.by("name")
        );

        when(projectRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Project>>any(),
                eq(pageable)
        )).thenReturn(new PageImpl<>(
                List.of(projectWithoutTasks, projectWithTasks),
                pageable,
                12
        ));

        TaskRepository.TaskProgressSummary summary =
                mock(TaskRepository.TaskProgressSummary.class);

        when(summary.getProjectId()).thenReturn(secondProjectId);
        when(summary.getTotalTasks()).thenReturn(3L);
        when(summary.getCompletedTasks()).thenReturn(1L);

        when(taskRepository.summarizeProgressByProjectIds(
                OWNER_ID,
                List.of(PROJECT_ID, secondProjectId),
                TaskStatus.COMPLETED
        )).thenReturn(List.of(summary));
        givenTrackedSeconds(PROJECT_ID, 3600);
        givenTrackedSeconds(secondProjectId, 5400);

        var result = service.list(
                OWNER_ID,
                null,
                null,
                null,
                pageable
        );

        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(2);

        var withoutTasks = result.getContent().get(0);

        assertThat(withoutTasks.getId()).isEqualTo(PROJECT_ID);
        assertThat(withoutTasks.getTargetHours()).isNull();
        assertThat(withoutTasks.getTaskProgress().getTotalTasks())
                .isZero();
        assertThat(withoutTasks.getTaskProgress().getCompletedTasks())
                .isZero();
        assertThat(withoutTasks.getTaskProgress().getPercent())
                .isEqualByComparingTo("0");
        assertThat(withoutTasks.getTimeTracking().getTrackedSeconds()).isEqualTo(3600);
        assertThat(withoutTasks.getTimeTracking().getTrackedHours())
                .isEqualByComparingTo("1.00");
        assertThat(withoutTasks.getTimeTracking().getUsagePercent()).isNull();

        var withTasks = result.getContent().get(1);

        assertThat(withTasks.getId()).isEqualTo(secondProjectId);
        assertThat(withTasks.getClient().getId())
                .isEqualTo(CLIENT_ID);
        assertThat(withTasks.getClient().getName())
                .isEqualTo("Acme");
        assertThat(withTasks.getTargetHours())
                .isEqualByComparingTo("2.00");
        assertThat(withTasks.getTaskProgress().getTotalTasks())
                .isEqualTo(3);
        assertThat(withTasks.getTaskProgress().getCompletedTasks())
                .isEqualTo(1);
        assertThat(withTasks.getTaskProgress().getPercent())
                .isEqualByComparingTo("33.33");
        assertThat(withTasks.getTimeTracking().getTrackedSeconds()).isEqualTo(5400);
        assertThat(withTasks.getTimeTracking().getTrackedHours())
                .isEqualByComparingTo("1.50");
        assertThat(withTasks.getTimeTracking().getUsagePercent())
                .isEqualByComparingTo("75.00");

        verify(taskRepository).summarizeProgressByProjectIds(
                OWNER_ID,
                List.of(PROJECT_ID, secondProjectId),
                TaskStatus.COMPLETED
        );
        verify(taskRepository, never()).findActiveByProjectIds(any(), any());
    }

    @Test
    void includesTasksOnlyWhenRequested() {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        Task task = new Task(project, "Design", 0);
        PageRequest pageable = PageRequest.of(
                0, 20, Sort.by(Sort.Direction.DESC, "createdAt")
        );

        when(projectRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Project>>any(),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(project), pageable, 1));
        when(taskRepository.findActiveByProjectIds(OWNER_ID, List.of(PROJECT_ID)))
                .thenReturn(List.of(task));
        givenTrackedSeconds(PROJECT_ID, 0);

        var response = service.list(
                OWNER_ID, null, null, null, pageable, true
        ).getContent().get(0);

        assertThat(response.getTasks()).extracting("name")
                .containsExactly("Design");
        verify(taskRepository).findActiveByProjectIds(OWNER_ID, List.of(PROJECT_ID));
    }

    @Test
    void emptyProjectPagePreservesPaginationWithoutQueryingTaskProgress() {
        PageRequest pageable = PageRequest.of(
                3,
                5,
                Sort.by("name")
        );

        when(projectRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Project>>any(),
                eq(pageable)
        )).thenReturn(new PageImpl<Project>(
                List.of(),
                pageable,
                12
        ));

        var result = service.list(
                OWNER_ID,
                null,
                null,
                null,
                pageable
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getNumber()).isEqualTo(3);
        assertThat(result.getSize()).isEqualTo(5);

        verifyNoInteractions(taskRepository);
    }

        @Test
    void changingStatusToArchivedSavesProjectAsInactive() {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.changeStatus(ProjectStatus.ACTIVE);

        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));

        when(projectRepository.save(project))
                .thenReturn(project);

        ChangeProjectStatusRequest request =
                ChangeProjectStatusRequest.builder()
                        .status(ProjectStatus.ARCHIVED)
                        .build();

        var response = service.changeStatus(
                OWNER_ID,
                PROJECT_ID,
                request
        );

        assertThat(response.getStatus())
                .isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(project.getIsActive()).isFalse();

        verify(projectRepository).save(project);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void cannotUpdateArchivedProject() {
        Project project = new Project(owner, client, "Website");
        project.changeStatus(ProjectStatus.ARCHIVED);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));

        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .clientId(CLIENT_ID)
                .name("Updated website")
                .build();

        assertThatThrownBy(() -> service.update(OWNER_ID, PROJECT_ID, request))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("โปรเจกต์ที่จัดเก็บแล้ว");
        assertThat(project.getName()).isEqualTo("Website");
        verifyNoInteractions(clientRepository);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void cannotUpdateCompletedProject() {
        Project project = new Project(owner, client, "Website");
        project.changeStatus(ProjectStatus.ACTIVE);
        project.changeStatus(ProjectStatus.COMPLETED);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));

        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .clientId(CLIENT_ID)
                .name("Updated website")
                .targetMinutes(240)
                .build();

        assertThatThrownBy(() -> service.update(OWNER_ID, PROJECT_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ไม่สามารถแก้ไขโปรเจกต์ที่เสร็จสิ้นแล้วได้");
        assertThat(project.getName()).isEqualTo("Website");
        assertThat(project.getTargetMinutes()).isNull();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verifyNoInteractions(clientRepository);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @ParameterizedTest
    @EnumSource(value = ProjectStatus.class, names = {"PLANNED", "ACTIVE", "ON_HOLD"})
    void updatesProjectDetailsInEditableStatuses(ProjectStatus status) {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        if (status != ProjectStatus.PLANNED) {
            project.changeStatus(ProjectStatus.ACTIVE);
            project.changeStatus(status);
        }
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(clientRepository.findByIdAndOwnerId(CLIENT_ID, OWNER_ID))
                .thenReturn(Optional.of(client));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service.update(OWNER_ID, PROJECT_ID,
                UpdateProjectRequest.builder()
                        .clientId(CLIENT_ID)
                        .name("Updated website")
                        .targetMinutes(240)
                        .build());

        assertThat(response.getName()).isEqualTo("Updated website");
        assertThat(project.getTargetMinutes()).isEqualTo(240);
        assertThat(project.getStatus()).isEqualTo(status);
        verify(projectRepository).save(project);
    }

    @Test
    void cannotRestoreArchivedProjectWhileClientIsArchived() {
        Project project = new Project(owner, client, "Website");
        project.changeStatus(ProjectStatus.ARCHIVED);
        client.setActive(false);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.changeStatus(
                OWNER_ID,
                PROJECT_ID,
                ChangeProjectStatusRequest.builder()
                        .status(ProjectStatus.ACTIVE)
                        .build()
        )).isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("ลูกค้าถูกจัดเก็บ");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void changingArchivedStatusToActiveSavesProjectAsActive() {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.changeStatus(ProjectStatus.ARCHIVED);

        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service.changeStatus(
                OWNER_ID,
                PROJECT_ID,
                ChangeProjectStatusRequest.builder()
                        .status(ProjectStatus.ACTIVE)
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(project.getIsActive()).isTrue();
        verify(projectRepository).save(project);
    }

    @Test
    void getsProjectDetailWithClientHoursAndTaskProgress() {
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.updateDetails("Website", "Description", null, null, null, 2160);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        TaskRepository.TaskProgressSummary summary = mock(TaskRepository.TaskProgressSummary.class);
        when(summary.getTotalTasks()).thenReturn(3L);
        when(summary.getCompletedTasks()).thenReturn(1L);
        when(taskRepository.summarizeProgressByProjectIds(
                OWNER_ID, List.of(PROJECT_ID), TaskStatus.COMPLETED))
                .thenReturn(List.of(summary));
        givenTrackedSeconds(PROJECT_ID, 36000);

        var result = service.getById(OWNER_ID, PROJECT_ID);

        assertThat(result.getId()).isEqualTo(PROJECT_ID);
        assertThat(result.getName()).isEqualTo("Website");
        assertThat(result.getDescription()).isEqualTo("Description");
        assertThat(result.getClient().getId()).isEqualTo(CLIENT_ID);
        assertThat(result.getClient().getName()).isEqualTo("Acme");
        assertThat(result.getTargetHours()).isEqualByComparingTo("36");
        assertThat(result.getTaskProgress().getTotalTasks()).isEqualTo(3);
        assertThat(result.getTaskProgress().getCompletedTasks()).isEqualTo(1);
        assertThat(result.getTaskProgress().getPercent()).isEqualByComparingTo("33.33");
        assertThat(result.getTimeTracking().getTrackedSeconds()).isEqualTo(36000);
        assertThat(result.getTimeTracking().getTrackedHours())
                .isEqualByComparingTo("10.00");
        assertThat(result.getTimeTracking().getUsagePercent())
                .isEqualByComparingTo("27.78");
        verify(taskRepository).summarizeProgressByProjectIds(
                OWNER_ID, List.of(PROJECT_ID), TaskStatus.COMPLETED);
    }

    @Test
    void projectDetailWithoutTasksOrTargetReturnsZeroProgressAndNullTarget() {
        Project project = new Project(owner, client, "Empty Project");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(taskRepository.summarizeProgressByProjectIds(
                OWNER_ID, List.of(PROJECT_ID), TaskStatus.COMPLETED))
                .thenReturn(List.of());
        givenTrackedSeconds(PROJECT_ID, 0);

        var result = service.getById(OWNER_ID, PROJECT_ID);

        assertThat(result.getTargetHours()).isNull();
        assertThat(result.getTaskProgress().getTotalTasks()).isZero();
        assertThat(result.getTaskProgress().getCompletedTasks()).isZero();
        assertThat(result.getTaskProgress().getPercent()).isEqualByComparingTo("0");
        assertThat(result.getTimeTracking().getTrackedSeconds()).isZero();
        assertThat(result.getTimeTracking().getTrackedHours())
                .isEqualByComparingTo("0.00");
        assertThat(result.getTimeTracking().getUsagePercent()).isNull();
    }

    private void givenTrackedSeconds(UUID projectId, long seconds) {
        when(timeEntryService.summarize(
                eq(OWNER_ID),
                argThat(filter -> projectId.equals(filter.getProjectId()))
        )).thenReturn(TimeEntrySummaryResponse.builder()
                .totalSeconds(seconds)
                .build());
    }
}

