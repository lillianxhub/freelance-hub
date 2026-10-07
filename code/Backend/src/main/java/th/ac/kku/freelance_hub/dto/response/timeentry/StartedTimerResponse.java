package th.ac.kku.freelance_hub.dto.response.timeentry;

import java.time.Instant;
import java.util.UUID;

import th.ac.kku.freelance_hub.domain.enums.TaskStatus;

/** Public response for starting a timer, with related data nested like entry details. */
public record StartedTimerResponse(
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

    public static StartedTimerResponse from(TimeEntryResponse source) {
        TaskSummary task = source.getTask() == null
                ? null
                : new TaskSummary(
                        source.getTask().id(),
                        source.getTaskName(),
                        source.getTask().status()
                );

        return new StartedTimerResponse(
                source.getId(),
                new ProjectSummary(
                        source.getProjectId(),
                        source.getProjectName()
                ),
                task,
                source.getStartedAt(),
                source.getEndedAt(),
                source.getDurationSeconds(),
                source.getDescription(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }

    public record ProjectSummary(UUID id, String name) {
    }

    public record TaskSummary(UUID id, String title, TaskStatus status) {
    }
}
