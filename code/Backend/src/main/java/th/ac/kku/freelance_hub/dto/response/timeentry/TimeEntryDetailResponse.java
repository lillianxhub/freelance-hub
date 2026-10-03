package th.ac.kku.freelance_hub.dto.response.timeentry;

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
        String description,
        Instant createdAt,
        Instant updatedAt
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
                source.getDescription(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }

    public record ProjectSummary(UUID id, String name) {}

    public record TaskSummary(UUID id, String title) {}
}
