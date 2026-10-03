package th.ac.kku.freelance_hub.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.progress.ProjectProgressThresholds;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.service.TimeEntryQueryService;
import th.ac.kku.freelance_hub.dto.request.timeentry.TimeEntryFilterRequest;
@Component
public class TimerStoppedProgressListener {

    private final ProjectRepository projectRepository;
    private final TimeEntryQueryService timeEntryQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public TimerStoppedProgressListener(
            ProjectRepository projectRepository,
            TimeEntryQueryService timeEntryQueryService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.projectRepository = projectRepository;
        this.timeEntryQueryService = timeEntryQueryService;
        this.eventPublisher = eventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onTimerStopped(TimerStoppedEvent event) {
        Project project = projectRepository
                .findByIdAndOwnerId(event.projectId(), event.ownerId())
                .orElse(null);

        if (project == null || project.getTargetMinutes() == null) {
            return;
        }

        long currentSeconds = timeEntryQueryService.summarize(
                event.ownerId(),
                TimeEntryFilterRequest.builder()
                        .projectId(event.projectId())
                        .build()
        ).getTotalSeconds();

        long previousSeconds = Math.max(
                0L,
                currentSeconds - event.durationSeconds()
        );

        for (int threshold : ProjectProgressThresholds.newlyReached(
                previousSeconds / 60,
                project.getTargetMinutes(),
                currentSeconds / 60,
                project.getTargetMinutes()
        )) {
            eventPublisher.publishEvent(
                    new ProjectProgressThresholdEvent(
                            event.ownerId(),
                            event.projectId(),
                            threshold
                    )
            );
        }
    }
}
