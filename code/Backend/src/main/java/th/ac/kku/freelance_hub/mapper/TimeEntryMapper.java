package th.ac.kku.freelance_hub.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;

/** Converts time-entry entities into data returned by the API. */
@Component
public class TimeEntryMapper {

    public TimeEntryResponse toResponse(TimeEntry entry) {
        Objects.requireNonNull(entry, "entry is required");

        Project project = entry.getProject();
        Client client = project.getClient();
        Task task = entry.getTask();

        return TimeEntryResponse.builder()
                .id(entry.getId())
                .clientId(client.getId())
                .clientName(client.getName())
                .projectId(project.getId())
                .projectName(project.getName())
                .taskId(task == null ? null : task.getId())
                .taskName(task == null ? null : task.getName())
                .description(entry.getDescription())
                .entryType(entry.getEntryType())
                .startedAt(entry.getStartedAt())
                .endedAt(entry.getEndedAt())
                .durationMinutes(entry.getDurationMinutes())
                .lockedAt(entry.getLockedAt())
                .running(entry.isRunning())
                .locked(entry.isLocked())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .version(entry.getVersion())
                .build();
    }
}
