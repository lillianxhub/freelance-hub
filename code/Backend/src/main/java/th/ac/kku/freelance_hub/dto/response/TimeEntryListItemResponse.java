package th.ac.kku.freelance_hub.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Compact time-entry data returned by the paginated list endpoint. */
public record TimeEntryListItemResponse(
        UUID id,
        ProjectSummary project,
        TaskSummary task,
        Instant startedAt,
        Instant endedAt,
        Long durationSeconds
) {

    public static TimeEntryListItemResponse from(TimeEntryResponse source) {
        ProjectSummary project = new ProjectSummary(
                source.getProjectId(),
                source.getProjectName()
        );
        TaskSummary task = source.getTaskId() == null
                ? null
                : new TaskSummary(source.getTaskId(), source.getTaskName());

        return new TimeEntryListItemResponse(
                source.getId(),
                project,
                task,
                source.getStartedAt(),
                source.getEndedAt(),
                source.getDurationSeconds()
        );
    }

    public record ProjectSummary(UUID id, String name) {}

    public record TaskSummary(UUID id, String title) {}
}
