package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.dto.request.CreateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.ReorderTaskRequest;
import th.ac.kku.freelance_hub.dto.response.TaskResponse;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;

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
    void cannotDeleteTaskWithRecordedTime() {
        TaskResponse response = taskService.create(
                owner.getId(), project.getId(), request("A", 0)
        );
        Task task = taskRepository.findById(response.getId()).orElseThrow();

        timeEntryRepository.saveAndFlush(
                TimeEntry.createManualWithDuration(
                        owner,
                        project,
                        task,
                        "Work",
                        Instant.parse("2026-01-01T10:00:00Z"),
                        30
                )
        );

        assertThatThrownBy(() -> taskService.delete(
                owner.getId(), project.getId(), task.getId()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("time entries");
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
}