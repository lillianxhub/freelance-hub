package th.ac.kku.freelance_hub.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@ExtendWith(OutputCaptureExtension.class)
class ProjectProgressThresholdListenerTest {

    @Test
    void receivesPublishedThresholdEvent(CapturedOutput output) {
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        try (var context = new AnnotationConfigApplicationContext(
                ProjectProgressThresholdListener.class
        )) {
            context.publishEvent(
                    new ProjectProgressThresholdEvent(ownerId, projectId, 80)
            );
        }

        assertThat(output.getAll()).contains(
                "Project " + projectId
                        + " owned by " + ownerId
                        + " reached 80% of its time target"
        );
    }
}