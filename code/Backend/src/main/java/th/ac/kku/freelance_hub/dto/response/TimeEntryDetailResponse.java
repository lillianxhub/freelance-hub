package th.ac.kku.freelance_hub.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Time-entry details exposed by create, detail, and update endpoints. */
public record TimeEntryDetailResponse(
        UUID id,
        ProjectSummary project,
        TaskSummary task,
        Instant startedAt,
        Instant endedAt,
        Long durationSeconds,
        String description
) {

    public static TimeEntryDetailResponse from(TimeEntryResponse source) {
        ProjectSummary project = new ProjectSummary(
                source.getProjectId(),
                source.getProjectName()
        );
        TaskSummary task = source.getTaskId() == null
                ? null
                : new TaskSummary(source.getTaskId(), source.getTaskName());

        return new TimeEntryDetailResponse(
                source.getId(),
                project,
                task,
                source.getStartedAt(),
                source.getEndedAt(),
                source.getDurationSeconds(),
                source.getDescription()
        );
    }

    public record ProjectSummary(UUID id, String name) {}

    public record TaskSummary(UUID id, String title) {}
}
