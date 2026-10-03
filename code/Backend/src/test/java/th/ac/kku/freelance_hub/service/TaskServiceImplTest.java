package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import jakarta.persistence.EntityManager;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.request.task.ChangeTaskStatusRequest;
import th.ac.kku.freelance_hub.dto.request.task.CreateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.task.ReorderTaskRequest;
import th.ac.kku.freelance_hub.dto.request.task.UpdateTaskRequest;
import th.ac.kku.freelance_hub.dto.response.task.TaskResponse;
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskServiceImplTest {

    @Autowired private TaskService taskService;
    @Autowired private UserRepository userRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TimeEntryRepository timeEntryRepository;
    @Autowired private EntityManager entityManager;

    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = userRepository.saveAndFlush(User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .passwordHash("test-hash")
                .build());

        Client client = clientRepository.saveAndFlush(
                new Client(owner, "Acme")
        );

        project = projectRepository.saveAndFlush(
                new Project(owner, client, "Website")
        );
    }

    @Test
    void latestTimeEntryTaskNameUsesMostRecentEntryForOwner() {
        Task firstTask = taskRepository.findById(taskService.create(
                owner.getId(), project.getId(), request("Design", 0)
        ).getId()).orElseThrow();
        Task latestTask = taskRepository.findById(taskService.create(
                owner.getId(), project.getId(), request("Review", 1)
        ).getId()).orElseThrow();

        timeEntryRepository.saveAndFlush(TimeEntry.createManualWithDurationSeconds(
                owner, project, firstTask, "Earlier work",
                Instant.parse("2026-01-01T09:00:00Z"), 600
        ));
        timeEntryRepository.saveAndFlush(TimeEntry.createManualWithDurationSeconds(
                owner, project, latestTask, "Latest work",
                Instant.parse("2026-01-02T09:00:00Z"), 600
        ));

        User otherOwner = userRepository.saveAndFlush(User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .passwordHash("test-hash")
                .build());
        Project otherProject = projectRepository.saveAndFlush(new Project(
                otherOwner,
                clientRepository.saveAndFlush(new Client(otherOwner, "Other client")),
                "Other project"
        ));
        Task otherTask = taskRepository.saveAndFlush(new Task(otherProject, "Other task", 0));
        timeEntryRepository.saveAndFlush(TimeEntry.createManualWithDurationSeconds(
                otherOwner, otherProject, otherTask, "Other user's latest work",
                Instant.parse("2026-01-03T09:00:00Z"), 600
        ));

        assertThat(taskService.getLatestTimeEntryTaskName(owner.getId()))
                .contains("Review");
    }

    @Test
    void latestTimeEntryTaskNameIsEmptyWhenLatestEntryHasNoTask() {
        assertThat(taskService.getLatestTimeEntryTaskName(owner.getId())).isEmpty();

        Task task = taskRepository.findById(taskService.create(
                owner.getId(), project.getId(), request("Design", 0)
        ).getId()).orElseThrow();
        timeEntryRepository.saveAndFlush(TimeEntry.createManualWithDurationSeconds(
                owner, project, task, "Earlier work",
                Instant.parse("2026-01-01T09:00:00Z"), 600
        ));
        timeEntryRepository.saveAndFlush(TimeEntry.createManualWithDurationSeconds(
                owner, project, null, "Project work",
                Instant.parse("2026-01-02T09:00:00Z"), 600
        ));

        assertThat(taskService.getLatestTimeEntryTaskName(owner.getId()))
                .isEmpty();
    }

    @Test
    void insertsAndReordersTasksWithoutDuplicateSortOrders() {
        taskService.create(owner.getId(), project.getId(), request("A", 0));
        TaskResponse b = taskService.create(
                owner.getId(), project.getId(), request("B", 1)
        );
        taskService.create(owner.getId(), project.getId(), request("C", 1));

        assertThat(orderedTasks())
                .extracting(Task::getName)
                .containsExactly("A", "C", "B");

        taskService.reorder(
                owner.getId(),
                project.getId(),
                b.getId(),
                ReorderTaskRequest.builder().sortOrder(0).build()
        );

        assertThat(orderedTasks())
                .extracting(Task::getName)
                .containsExactly("B", "A", "C");

        assertThat(orderedTasks())
                .extracting(Task::getSortOrder)
                .containsExactly(0, 1, 2);
    }

    @Test
    void otherOwnerCannotReadTask() {
        TaskResponse task = taskService.create(
                owner.getId(), project.getId(), request("A", 0)
        );

        assertThatThrownBy(() -> taskService.getById(
                UUID.randomUUID(), project.getId(), task.getId()
        )).isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void softDeletePreservesTaskAndRecordedTime() {
        TaskResponse response = taskService.create(
                owner.getId(), project.getId(), request("A", 0)
        );
        Task task = taskRepository.findById(response.getId()).orElseThrow();
        TimeEntry entry = timeEntryRepository.saveAndFlush(
                TimeEntry.createManualWithDurationSeconds(
                        owner,
                        project,
                        task,
                        "Work",
                        Instant.parse("2026-01-01T10:00:00Z"),
                        1800
                )
        );
        UUID ownerId = owner.getId();
        UUID projectId = project.getId();
        UUID taskId = task.getId();
        UUID entryId = entry.getId();

        taskService.delete(ownerId, taskId);
        entityManager.flush();
        entityManager.clear();

        Task storedTask = taskRepository.findById(taskId).orElseThrow();
        assertThat(storedTask.getIsActive()).isFalse();
        assertThat(storedTask.getDeletedAt()).isNotNull();
        assertThat(storedTask.getName()).isEqualTo("A");
        TimeEntry storedEntry = timeEntryRepository.findById(entryId).orElseThrow();
        assertThat(storedEntry.getTask().getId()).isEqualTo(taskId);
        assertThat(storedEntry.getDescription()).isEqualTo("Work");

        assertThat(taskService.list(ownerId, projectId, true, PageRequest.of(0, 10))
                .getContent()).isEmpty();
        assertThat(taskService.list(ownerId, projectId, false, PageRequest.of(0, 10))
                .getContent()).extracting(TaskResponse::getId).containsExactly(taskId);
        assertThatThrownBy(() -> taskService.getById(ownerId, taskId))
                .isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> taskService.getById(ownerId, projectId, taskId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void canCreateTaskWhileProjectIsOnHold() {
        project.changeStatus(ProjectStatus.ACTIVE);
        project.changeStatus(ProjectStatus.ON_HOLD);
        projectRepository.saveAndFlush(project);

        taskService.create(owner.getId(), project.getId(), request("A", 0));

        assertThat(orderedTasks())
                .extracting(Task::getName)
                .containsExactly("A");
    }

    @Test
    void cannotCreateTaskWhenProjectIsCompleted() {
        project.changeStatus(ProjectStatus.ACTIVE);
        project.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.create(
                owner.getId(), project.getId(), request("A", 0)
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotCreateTaskWhenProjectIsArchived() {
        project.changeStatus(ProjectStatus.ARCHIVED);
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.create(
                owner.getId(), project.getId(), request("A", 0)
        )).isInstanceOf(IllegalStateException.class);
    }
    

    private CreateTaskRequest request(String name, int sortOrder) {
        return CreateTaskRequest.builder()
                .name(name)
                .sortOrder(sortOrder)
                .build();
    }

    private java.util.List<Task> orderedTasks() {
        return taskRepository
                .findAllByProjectIdAndProjectOwnerIdOrderBySortOrderAsc(
                        project.getId(),
                        owner.getId()
                );
    }

    @Test
    void listFiltersActiveTasksBeforePaginationAndKeepsProjectScope() {
        Task first = taskRepository.saveAndFlush(
                new Task(project, "First Active", 0));
        Task inactive = new Task(project, "Inactive", 1);
        ReflectionTestUtils.setField(inactive, "isActive", false);
        ReflectionTestUtils.setField(inactive, "deletedAt", Instant.now());
        inactive = taskRepository.saveAndFlush(inactive);
        Task second = taskRepository.saveAndFlush(
                new Task(project, "Second Active", 2));

        Project otherProject = projectRepository.saveAndFlush(
                new Project(owner, project.getClient(), "Other Project"));
        taskRepository.saveAndFlush(new Task(otherProject, "Other Task", 0));

        var activePage = taskService.list(
                owner.getId(), project.getId(), true, PageRequest.of(0, 1));
        assertThat(activePage.getContent())
                .extracting(TaskResponse::getId)
                .containsExactly(first.getId());
        assertThat(activePage.getTotalElements()).isEqualTo(2);
        assertThat(activePage.getTotalPages()).isEqualTo(2);

        var nextPage = taskService.list(
                owner.getId(), project.getId(), true, PageRequest.of(1, 1));
        assertThat(nextPage.getContent())
                .extracting(TaskResponse::getId)
                .containsExactly(second.getId());

        var inactivePage = taskService.list(
                owner.getId(), project.getId(), false, PageRequest.of(0, 10));
        assertThat(inactivePage.getContent())
                .extracting(TaskResponse::getId)
                .containsExactly(inactive.getId());
        assertThat(inactivePage.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listRejectsOtherOwnersForBothActiveFilters() {
        for (boolean isActive : new boolean[] {true, false}) {
            assertThatThrownBy(() -> taskService.list(
                    UUID.randomUUID(), project.getId(),
                    isActive, PageRequest.of(0, 10)
            )).isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Test
    void getsTaskDetailByTaskIdOnlyForItsProjectOwner() {
        Task task = new Task(project, "Task Detail", 0);
        task.updateDetails("Task Detail", "Description");
        task = taskRepository.saveAndFlush(task);

        TaskResponse result = taskService.getById(owner.getId(), task.getId());
        assertThat(result.getId()).isEqualTo(task.getId());
        assertThat(result.getProjectId()).isEqualTo(project.getId());
        assertThat(result.getName()).isEqualTo("Task Detail");
        assertThat(result.getDescription()).isEqualTo("Description");

        UUID taskId = task.getId();
        assertThatThrownBy(() -> taskService.getById(UUID.randomUUID(), taskId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void taskDetailReturnsNotFoundForUnknownTaskId() {
        assertThatThrownBy(() -> taskService.getById(owner.getId(), UUID.randomUUID()))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void taskDetailReturnsNotFoundWhenProjectIsSoftDeleted() {
        Task task = taskRepository.saveAndFlush(new Task(project, "Task", 0));
        project.archive();
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.getById(owner.getId(), task.getId()))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void updatesTaskByIdForOwnerWithoutChangingStatusOrOrder() {
        Task task = taskRepository.saveAndFlush(new Task(project, "Original", 0));
        TaskResponse result = taskService.update(owner.getId(), task.getId(),
                UpdateTaskRequest.builder()
                        .name("Renamed")
                        .description("Updated detail")
                        .build());

        assertThat(result.getId()).isEqualTo(task.getId());
        assertThat(result.getProjectId()).isEqualTo(project.getId());
        assertThat(result.getName()).isEqualTo("Renamed");
        assertThat(result.getDescription()).isEqualTo("Updated detail");
        assertThat(result.getStatus()).isEqualTo(task.getStatus());
        assertThat(result.getSortOrder()).isEqualTo(0);
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getName())
                .isEqualTo("Renamed");
    }

    @Test
    void canUpdateTaskByIdWhileProjectIsOnHold() {
        Task task = taskRepository.saveAndFlush(new Task(project, "Original", 0));
        project.changeStatus(ProjectStatus.ACTIVE);
        project.changeStatus(ProjectStatus.ON_HOLD);
        projectRepository.saveAndFlush(project);

        TaskResponse result = taskService.update(owner.getId(), task.getId(),
                UpdateTaskRequest.builder().name("Renamed").build());
        assertThat(result.getName()).isEqualTo("Renamed");
        assertThat(result.getDescription()).isNull();
    }

    @Test
    void updateByTaskIdRejectsOtherOwnerAndUnknownTask() {
        Task task = taskRepository.saveAndFlush(new Task(project, "Original", 0));
        UpdateTaskRequest request = UpdateTaskRequest.builder().name("Renamed").build();

        assertThatThrownBy(() -> taskService.update(UUID.randomUUID(), task.getId(), request))
                .isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> taskService.update(owner.getId(), UUID.randomUUID(), request))
                .isInstanceOf(TaskNotFoundException.class);
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getName())
                .isEqualTo("Original");
    }

    @Test
    void cannotUpdateTaskByIdWhenProjectIsCompleted() {
        Task task = taskRepository.saveAndFlush(new Task(project, "Original", 0));
        project.changeStatus(ProjectStatus.ACTIVE);
        project.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.update(owner.getId(), task.getId(),
                UpdateTaskRequest.builder().name("Renamed").build()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(task.getName()).isEqualTo("Original");
    }

    @Test
    void cannotUpdateTaskByIdWhenProjectIsArchived() {
        Task task = taskRepository.saveAndFlush(new Task(project, "Original", 0));
        project.changeStatus(ProjectStatus.ARCHIVED);
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.update(owner.getId(), task.getId(),
                UpdateTaskRequest.builder().name("Renamed").build()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(task.getName()).isEqualTo("Original");
    }

    @Test
    void cannotUpdateTaskByIdWhenProjectIsSoftDeleted() {
        Task task = taskRepository.saveAndFlush(new Task(project, "Original", 0));
        project.archive();
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.update(owner.getId(), task.getId(),
                UpdateTaskRequest.builder().name("Renamed").build()))
                .isInstanceOf(TaskNotFoundException.class);
        assertThat(task.getName()).isEqualTo("Original");
    }

    @ParameterizedTest
    @CsvSource({"OPEN, IN_PROGRESS", "OPEN, COMPLETED", "IN_PROGRESS, COMPLETED"})
    void changeTaskStatusAllowsExistingForwardTransitions(TaskStatus before, TaskStatus after) {
        Task task = new Task(project, "Task", 0);
        if (before == TaskStatus.IN_PROGRESS) {
            task.start();
        }
        task = taskRepository.saveAndFlush(task);

        TaskResponse result = taskService.changeStatus(owner.getId(), task.getId(),
                ChangeTaskStatusRequest.builder().status(after).build());
        assertThat(result.getStatus()).isEqualTo(after);
        Task stored = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(after);
        if (after == TaskStatus.COMPLETED) {
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(stored.getCompletedAt()).isEqualTo(result.getCompletedAt());
        } else {
            assertThat(result.getCompletedAt()).isNull();
        }
    }

    @ParameterizedTest
    @CsvSource({"COMPLETED, OPEN", "IN_PROGRESS, OPEN"})
    void changeTaskStatusRejectsTransitionsToOpen(TaskStatus before, TaskStatus after) {
        Task task = new Task(project, "Task", 0);
        if (before == TaskStatus.COMPLETED) {
            task.complete(Instant.parse("2026-01-01T10:00:00Z"));
        } else {
            task.start();
        }
        task = taskRepository.saveAndFlush(task);
        UUID taskId = task.getId();
        Instant originalCompletedAt = task.getCompletedAt();

        assertThatThrownBy(() -> taskService.changeStatus(owner.getId(), taskId,
                ChangeTaskStatusRequest.builder().status(after).build()))
                .isInstanceOf(IllegalStateException.class);
        Task stored = taskRepository.findById(taskId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(before);
        assertThat(stored.getCompletedAt()).isEqualTo(originalCompletedAt);
    }

    @Test
    void changeTaskStatusReopensCompletedTaskAndCanCompleteAgain() {
        Instant firstCompletedAt = Instant.parse("2026-01-01T10:00:00Z");
        Task task = new Task(project, "Task", 0);
        task.complete(firstCompletedAt);
        UUID taskId = taskRepository.saveAndFlush(task).getId();

        TaskResponse reopened = taskService.changeStatus(owner.getId(), taskId,
                ChangeTaskStatusRequest.builder().status(TaskStatus.IN_PROGRESS).build());
        assertThat(reopened.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(reopened.getCompletedAt()).isNull();

        entityManager.clear();
        Task stored = taskRepository.findById(taskId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(stored.getCompletedAt()).isNull();

        TaskResponse completedAgain = taskService.changeStatus(owner.getId(), taskId,
                ChangeTaskStatusRequest.builder().status(TaskStatus.COMPLETED).build());
        assertThat(completedAgain.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completedAgain.getCompletedAt()).isAfter(firstCompletedAt);
    }

    @Test
    void changeTaskStatusCannotReopenTaskInArchivedProject() {
        Instant completedAt = Instant.parse("2026-01-01T10:00:00Z");
        Task task = new Task(project, "Task", 0);
        task.complete(completedAt);
        UUID taskId = taskRepository.saveAndFlush(task).getId();
        project.changeStatus(ProjectStatus.ARCHIVED);
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.changeStatus(owner.getId(), taskId,
                ChangeTaskStatusRequest.builder().status(TaskStatus.IN_PROGRESS).build()))
                .isInstanceOf(IllegalStateException.class);

        entityManager.clear();
        Task stored = taskRepository.findById(taskId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(stored.getCompletedAt()).isEqualTo(completedAt);
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void changeTaskStatusKeepsCompletionTimeWhenStatusIsUnchanged(TaskStatus status) {
        Task task = new Task(project, "Task", 0);
        if (status == TaskStatus.IN_PROGRESS) {
            task.start();
        } else if (status == TaskStatus.COMPLETED) {
            task.complete(Instant.parse("2026-01-01T10:00:00Z"));
        }
        task = taskRepository.saveAndFlush(task);
        Instant originalCompletedAt = task.getCompletedAt();

        TaskResponse result = taskService.changeStatus(owner.getId(), task.getId(),
                ChangeTaskStatusRequest.builder().status(status).build());
        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.getCompletedAt()).isEqualTo(originalCompletedAt);
    }

    @ParameterizedTest
    @EnumSource(value = ProjectStatus.class, names = {"COMPLETED", "ARCHIVED"})
    void changeTaskStatusRejectsProjectsThatForbidEditing(ProjectStatus projectStatus) {
        Task task = taskRepository.saveAndFlush(new Task(project, "Task", 0));
        if (projectStatus == ProjectStatus.COMPLETED) {
            project.changeStatus(ProjectStatus.ACTIVE);
        }
        project.changeStatus(projectStatus);
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.changeStatus(owner.getId(), task.getId(),
                ChangeTaskStatusRequest.builder().status(TaskStatus.IN_PROGRESS).build()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
    }

    @Test
    void changeTaskStatusRejectsOtherOwnerAndUnknownTask() {
        Task task = taskRepository.saveAndFlush(new Task(project, "Task", 0));
        ChangeTaskStatusRequest request = ChangeTaskStatusRequest.builder()
                .status(TaskStatus.IN_PROGRESS).build();

        assertThatThrownBy(() -> taskService.changeStatus(UUID.randomUUID(), task.getId(), request))
                .isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> taskService.changeStatus(owner.getId(), UUID.randomUUID(), request))
                .isInstanceOf(TaskNotFoundException.class);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
    }

    @Test
    void changeTaskStatusRejectsSoftDeletedProject() {
        Task task = taskRepository.saveAndFlush(new Task(project, "Task", 0));
        project.archive();
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.changeStatus(owner.getId(), task.getId(),
                ChangeTaskStatusRequest.builder().status(TaskStatus.IN_PROGRESS).build()))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void deletingAndCreatingTasksKeepsActiveSortOrdersUnique() {
        TaskResponse a = taskService.create(owner.getId(), project.getId(), request("A", 0));
        TaskResponse b = taskService.create(owner.getId(), project.getId(), request("B", 1));
        TaskResponse c = taskService.create(owner.getId(), project.getId(), request("C", 2));

        taskService.delete(owner.getId(), b.getId());
        entityManager.clear();
        assertActiveTaskOrder("A", "C");
        TaskResponse d = taskService.create(owner.getId(), project.getId(), request("D", 1));
        entityManager.clear();
        assertActiveTaskOrder("A", "D", "C");

        taskService.reorder(owner.getId(), project.getId(), c.getId(),
                ReorderTaskRequest.builder().sortOrder(0).build());
        entityManager.clear();
        assertActiveTaskOrder("C", "A", "D");

        taskService.delete(owner.getId(), project.getId(), a.getId());
        entityManager.clear();
        assertActiveTaskOrder("C", "D");
        taskService.create(owner.getId(), project.getId(), request("E", 0));
        entityManager.clear();
        assertActiveTaskOrder("E", "C", "D");

        assertThat(taskRepository.countByProjectId(project.getId())).isEqualTo(5);
        var all = orderedTasks();
        assertThat(all).extracting(Task::getSortOrder).containsExactly(0, 1, 2, 3, 4);
        assertThat(all).extracting(Task::getName).containsExactly("E", "C", "D", "A", "B");
        assertThat(taskService.list(owner.getId(), project.getId(), false, PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(2);
        assertThat(taskRepository.findById(d.getId())).isPresent();
    }

    @Test
    void canCreateTaskAfterDeletingTheOnlyActiveTask() {
        TaskResponse deleted = taskService.create(owner.getId(), project.getId(), request("A", 0));
        taskService.delete(owner.getId(), deleted.getId());
        taskService.create(owner.getId(), project.getId(), request("B", 0));

        assertActiveTaskOrder("B");
        assertThat(orderedTasks()).extracting(Task::getSortOrder).containsExactly(0, 1);
        assertThat(orderedTasks()).extracting(Task::getName).containsExactly("B", "A");
    }

    @Test
    void softDeleteRejectsOtherOwnerAndUnknownTask() {
        TaskResponse task = taskService.create(owner.getId(), project.getId(), request("A", 0));
        assertThatThrownBy(() -> taskService.delete(UUID.randomUUID(), task.getId()))
                .isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> taskService.delete(owner.getId(), UUID.randomUUID()))
                .isInstanceOf(TaskNotFoundException.class);
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getIsActive()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ProjectStatus.class, names = {"COMPLETED", "ARCHIVED"})
    void softDeleteRejectsProjectsThatForbidEditing(ProjectStatus status) {
        TaskResponse task = taskService.create(owner.getId(), project.getId(), request("A", 0));
        if (status == ProjectStatus.COMPLETED) {
            project.changeStatus(ProjectStatus.ACTIVE);
        }
        project.changeStatus(status);
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.delete(owner.getId(), task.getId()))
                .isInstanceOf(IllegalStateException.class);
        Task stored = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(stored.getIsActive()).isTrue();
        assertThat(stored.getDeletedAt()).isNull();
    }

    @Test
    void softDeleteRejectsSoftDeletedProject() {
        TaskResponse task = taskService.create(owner.getId(), project.getId(), request("A", 0));
        project.archive();
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.delete(owner.getId(), task.getId()))
                .isInstanceOf(TaskNotFoundException.class);
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getIsActive()).isTrue();
    }

    @Test
    void deletedTaskCannotBeMutatedOrDeletedAgain() {
        TaskResponse task = taskService.create(owner.getId(), project.getId(), request("A", 0));
        UUID taskId = task.getId();
        taskService.delete(owner.getId(), taskId);
        Instant deletedAt = taskRepository.findById(taskId).orElseThrow().getDeletedAt();

        assertThatThrownBy(() -> taskService.delete(owner.getId(), taskId))
                .isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> taskService.update(owner.getId(), taskId,
                UpdateTaskRequest.builder().name("Changed").build()))
                .isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> taskService.changeStatus(owner.getId(), taskId,
                ChangeTaskStatusRequest.builder().status(TaskStatus.COMPLETED).build()))
                .isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> taskService.start(owner.getId(), project.getId(), taskId))
                .isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> taskService.complete(owner.getId(), project.getId(), taskId))
                .isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> taskService.reorder(owner.getId(), project.getId(), taskId,
                ReorderTaskRequest.builder().sortOrder(0).build()))
                .isInstanceOf(TaskNotFoundException.class);
        assertThat(taskRepository.findById(taskId).orElseThrow().getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void reorderRejectsTaskFromAnotherProject() {
        TaskResponse task = taskService.create(owner.getId(), project.getId(), request("A", 0));
        Project otherProject = projectRepository.saveAndFlush(
                new Project(owner, project.getClient(), "Other project"));

        assertThatThrownBy(() -> taskService.reorder(owner.getId(), otherProject.getId(),
                task.getId(), ReorderTaskRequest.builder().sortOrder(0).build()))
                .isInstanceOf(TaskNotFoundException.class);
        assertThat(orderedTasks()).extracting(Task::getName).containsExactly("A");
    }

    @Test
    void reorderRejectsAnotherOwner() {
        TaskResponse task = taskService.create(owner.getId(), project.getId(), request("A", 0));

        assertThatThrownBy(() -> taskService.reorder(UUID.randomUUID(), project.getId(),
                task.getId(), ReorderTaskRequest.builder().sortOrder(0).build()))
                .isInstanceOf(ProjectNotFoundException.class);
        assertThat(orderedTasks()).extracting(Task::getSortOrder).containsExactly(0);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {-1, 2})
    void reorderRejectsPositionOutsideActiveList(int position) {
        TaskResponse task = taskService.create(owner.getId(), project.getId(), request("A", 0));
        taskService.create(owner.getId(), project.getId(), request("B", 1));

        assertThatThrownBy(() -> taskService.reorder(owner.getId(), project.getId(),
                task.getId(), ReorderTaskRequest.builder().sortOrder(position).build()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(orderedTasks()).extracting(Task::getName).containsExactly("A", "B");
        assertThat(orderedTasks()).extracting(Task::getSortOrder).containsExactly(0, 1);
    }

    @ParameterizedTest
    @EnumSource(value = ProjectStatus.class, names = {"COMPLETED", "ARCHIVED"})
    void reorderRejectsProjectStateThatCannotEditTasks(ProjectStatus status) {
        TaskResponse task = taskService.create(owner.getId(), project.getId(), request("A", 0));
        project.changeStatus(ProjectStatus.ACTIVE);
        project.changeStatus(status);
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> taskService.reorder(owner.getId(), project.getId(),
                task.getId(), ReorderTaskRequest.builder().sortOrder(0).build()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(orderedTasks()).extracting(Task::getSortOrder).containsExactly(0);
    }
    private void assertActiveTaskOrder(String... names) {
        var tasks = taskService.list(owner.getId(), project.getId(), true, PageRequest.of(0, 100));
        assertThat(tasks.getContent()).extracting(TaskResponse::getName).containsExactly(names);
        assertThat(tasks.getContent()).extracting(TaskResponse::getSortOrder)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, names.length)
                        .boxed().toList());
    }
}
