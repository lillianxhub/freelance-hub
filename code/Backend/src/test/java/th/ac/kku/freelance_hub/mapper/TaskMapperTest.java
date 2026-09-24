package th.ac.kku.freelance_hub.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.dto.response.TaskResponse;

class TaskMapperTest {

    private final TaskMapper mapper = new TaskMapper();

    @Test
    void mapsCompletedTaskAndItsProjectIdToResponse() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .build();
        Client client = new Client(owner, "Acme");

        UUID projectId = UUID.randomUUID();
        Project project = new Project(owner, client, "Website");
        ReflectionTestUtils.setField(project, "id", projectId);

        UUID taskId = UUID.randomUUID();
        Task task = new Task(project, "Build homepage", 2);
        task.updateDetails("Build homepage", "Create the landing page");
        task.start();

        Instant completedAt = Instant.parse("2026-09-25T09:00:00Z");
        task.complete(completedAt);
        ReflectionTestUtils.setField(task, "id", taskId);

        TaskResponse response = mapper.toResponse(task);

        assertThat(response.getId()).isEqualTo(taskId);
        assertThat(response.getProjectId()).isEqualTo(projectId);
        assertThat(response.getName()).isEqualTo("Build homepage");
        assertThat(response.getDescription()).isEqualTo("Create the landing page");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.getSortOrder()).isEqualTo(2);
        assertThat(response.getCompletedAt()).isEqualTo(completedAt);
    }
}