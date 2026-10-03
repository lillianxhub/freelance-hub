package th.ac.kku.freelance_hub.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

class ProjectStateTest {

    @Test
    void followsStateRulesAcrossProjectLifecycle() {
        Project project = newProject();

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);
        assertThat(project.canTrackTime()).isFalse();
        assertThat(project.canEditTasks()).isTrue();

        project.changeStatus(ProjectStatus.ACTIVE);
        assertThat(project.canTrackTime()).isTrue();
        assertThat(project.canEditTasks()).isTrue();

        project.changeStatus(ProjectStatus.ON_HOLD);
        assertThat(project.canTrackTime()).isFalse();
        assertThat(project.canEditTasks()).isTrue();

        project.changeStatus(ProjectStatus.ACTIVE);
        project.changeStatus(ProjectStatus.COMPLETED);
        assertThat(project.canTrackTime()).isFalse();
        assertThat(project.canEditTasks()).isFalse();

        project.archive();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(project.canTrackTime()).isFalse();
        assertThat(project.canEditTasks()).isFalse();
    }   

    @Test
    void rejectsInvalidTransitionWithoutChangingStatus() {
        Project project = newProject();

        assertThatThrownBy(() -> project.changeStatus(ProjectStatus.COMPLETED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);

        project.archive();
        assertThatThrownBy(() -> project.changeStatus(ProjectStatus.ON_HOLD))
                .isInstanceOf(IllegalStateException.class);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void restoresArchivedProjectToActiveOrPlanned() {
        for (ProjectStatus nextStatus : new ProjectStatus[] {
                ProjectStatus.ACTIVE, ProjectStatus.PLANNED
        }) {
            Project project = newProject();
            project.changeStatus(ProjectStatus.ARCHIVED);
            assertThat(project.getIsActive()).isFalse();

            project.changeStatus(nextStatus);

            assertThat(project.getStatus()).isEqualTo(nextStatus);
            assertThat(project.getIsActive()).isTrue();
        }
    }

    @Test
    void acceptsTheCurrentStatusAgain() {
        Project project = newProject();

        project.changeStatus(ProjectStatus.PLANNED);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);

        project.changeStatus(ProjectStatus.ACTIVE);
        project.changeStatus(ProjectStatus.ACTIVE);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    private Project newProject() {
        User owner = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .email("owner@example.com")
                .passwordHash("hashed-password")
                .build();

        Client client = new Client(owner, "Example Client");
        return new Project(owner, client, "Example Project");
    }
}
