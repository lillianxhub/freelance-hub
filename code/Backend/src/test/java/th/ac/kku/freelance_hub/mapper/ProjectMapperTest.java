package th.ac.kku.freelance_hub.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.response.ProjectResponse;

class ProjectMapperTest {

    private final ProjectMapper mapper = new ProjectMapper();

    @Test
    void mapsProjectAndClientIdToResponse() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .build();

        UUID clientId = UUID.randomUUID();
        Client client = new Client(owner, "Acme");
        ReflectionTestUtils.setField(client, "id", clientId);

        UUID projectId = UUID.randomUUID();
        Project project = new Project(owner, client, "Website");
        project.updateDetails(
                "Website",
                "Landing page",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "#336699",
                120
        );
        project.changeStatus(ProjectStatus.ACTIVE);
        ReflectionTestUtils.setField(project, "id", projectId);

        ProjectResponse response = mapper.toResponse(project);

        assertThat(response.getId()).isEqualTo(projectId);
        assertThat(response.getClientId()).isEqualTo(clientId);
        assertThat(response.getName()).isEqualTo("Website");
        assertThat(response.getTargetMinutes()).isEqualTo(120);
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }
}