package th.ac.kku.freelance_hub.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProjectProgressThresholdListener {

    private static final Logger log =
            LoggerFactory.getLogger(ProjectProgressThresholdListener.class);

    @EventListener
    public void onThresholdReached(ProjectProgressThresholdEvent event) {
        log.info(
                "Project {} owned by {} reached {}% of its time target",
                event.projectId(),
                event.ownerId(),
                event.thresholdPercent()
        );
    }
}