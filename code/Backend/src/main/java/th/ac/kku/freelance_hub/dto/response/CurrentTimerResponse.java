package th.ac.kku.freelance_hub.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Current timer state returned to the authenticated user. */
public record CurrentTimerResponse(
        boolean running,
        CurrentTimeEntry timeEntry
) {

    public static CurrentTimerResponse active(TimeEntryResponse source) {
        ProjectSummary project = new ProjectSummary(
                source.getProjectId(),
                source.getProjectName()
        );
        TaskSummary task = source.getTaskId() == null
                ? null
                : new TaskSummary(source.getTaskId(), source.getTaskName());

        return new CurrentTimerResponse(
                true,
                new CurrentTimeEntry(
                        source.getId(),
                        project,
                        task,
                        source.getStartedAt(),
                        source.getDescription()
                )
        );
    }

    public static CurrentTimerResponse inactive() {
        return new CurrentTimerResponse(false, null);
    }

    public record CurrentTimeEntry(
            UUID id,
            ProjectSummary project,
            TaskSummary task,
            Instant startedAt,
            String description
    ) {
    }

    public record ProjectSummary(UUID id, String name) {
    }

    public record TaskSummary(UUID id, String title) {
    }
}
