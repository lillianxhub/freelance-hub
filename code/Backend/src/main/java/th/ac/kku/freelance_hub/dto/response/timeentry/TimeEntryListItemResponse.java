package th.ac.kku.freelance_hub.dto.response.timeentry;

import java.time.Instant;
import java.util.UUID;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;

/** Compact time-entry data returned by the paginated list endpoint. */
public record TimeEntryListItemResponse(
        UUID id,
        ProjectSummary project,
        TaskSummary task,
        Instant startedAt,
        Instant endedAt,
        Long durationSeconds,
        String description
) {

    public static TimeEntryListItemResponse from(TimeEntryResponse source) {
        ProjectSummary project = new ProjectSummary(
                source.getProjectId(),
                source.getProjectName(),
                source.getProjectStatus()
        );
        TaskSummary task = source.getTaskId() == null
                ? null
                : new TaskSummary(
                        source.getTaskId(),
                        source.getTaskName(),
                        source.getTask().status()
                );

        return new TimeEntryListItemResponse(
                source.getId(),
                project,
                task,
                source.getStartedAt(),
                source.getEndedAt(),
                source.getDurationSeconds(),
                source.getDescription()
        );
    }

    public record ProjectSummary(UUID id, String name, ProjectStatus status) {}

    public record TaskSummary(UUID id, String title, TaskStatus status) {}
}
