package th.ac.kku.freelance_hub.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.exception.InvalidArgumentException;
import th.ac.kku.freelance_hub.exception.InvalidStateException;

class TaskTest {
    private final User owner = User.builder().email("owner@example.com").build();
    private final Project project = new Project(owner, new Client(owner, "Client"), "Project");
    private final Instant now = Instant.parse("2026-10-10T00:00:00Z");

    @Test
    void startsOnceAndCompletedTaskCannotStartAgain() {
        Task task = new Task(project, "  Work  ", 0);
        assertThat(task.getName()).isEqualTo("Work");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(task.start()).isTrue();
        assertThat(task.start()).isFalse();
        task.complete(now);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isEqualTo(now);
        assertThatThrownBy(task::start).isInstanceOf(InvalidStateException.class);
        assertThatThrownBy(() -> task.complete(now)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void reopensCompletedTaskAndClearsCompletionTime() {
        Task task = new Task(project, "Work", 0);
        task.start();
        task.changeStatus(TaskStatus.COMPLETED, now);
        task.changeStatus(TaskStatus.COMPLETED, now.plusSeconds(10));
        assertThat(task.getCompletedAt()).isEqualTo(now);
        task.changeStatus(TaskStatus.IN_PROGRESS, now);
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThatThrownBy(() -> task.changeStatus(TaskStatus.OPEN, now))
                .isInstanceOf(InvalidStateException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void requiresTaskName(String name) {
        assertThatThrownBy(() -> new Task(project, name, 0))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void reorderAndDeletePreserveDomainConstraints() {
        Task task = new Task(project, "Work", 0);
        assertThatThrownBy(() -> task.reorder(-1)).isInstanceOf(InvalidArgumentException.class);
        assertThat(task.getSortOrder()).isZero();
        task.reorder(3);
        assertThat(task.getSortOrder()).isEqualTo(3);
        task.softDelete();
        Instant deletedAt = task.getDeletedAt();
        task.softDelete();
        assertThat(task.getIsActive()).isFalse();
        assertThat(task.getDeletedAt()).isEqualTo(deletedAt);
    }
}
