package th.ac.kku.freelance_hub.dto.response.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.response.task.TaskResponse;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectListItemResponse {

    private UUID id;
    private String name;
    private String description;
    private String color;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal targetHours;

    private ClientSummary client;
    private TaskProgress taskProgress;
    private TimeTracking timeTracking;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Returned only by GET /api/projects when include=tasks; GET /api/projects/{id} does not support include and omits this field", accessMode = Schema.AccessMode.READ_ONLY)
    private List<TaskResponse> tasks;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientSummary {
        private UUID id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskProgress {
        private long totalTasks;
        private long completedTasks;
        private BigDecimal percent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeTracking {
        private long trackedSeconds;
        private BigDecimal trackedHours;
        private BigDecimal usagePercent;
    }
}
