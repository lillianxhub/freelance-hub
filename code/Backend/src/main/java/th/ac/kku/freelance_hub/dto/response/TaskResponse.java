package th.ac.kku.freelance_hub.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private UUID id;

    private UUID projectId;

    private String name;

    private String description;

    private TaskStatus status;

    private int sortOrder;

    private Instant completedAt;

    private Instant createdAt;

    private Instant updatedAt;

    private Long version;
}