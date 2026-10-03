package th.ac.kku.freelance_hub.dto.response.client;

import java.util.UUID;

import th.ac.kku.freelance_hub.domain.enums.TaskStatus;

/** Selected Task fields included under its Project on request. */
public record ClientTaskSummaryResponse(UUID id, String name, TaskStatus status) {
}
