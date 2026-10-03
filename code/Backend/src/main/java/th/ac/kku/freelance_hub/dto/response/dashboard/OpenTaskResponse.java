package th.ac.kku.freelance_hub.dto.response.dashboard;

import java.util.UUID;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;

public record OpenTaskResponse(
        UUID id,
        UUID projectId,
        String name,
        String projectName,
        TaskStatus status
) {}