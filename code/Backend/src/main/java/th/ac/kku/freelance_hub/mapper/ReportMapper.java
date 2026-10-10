package th.ac.kku.freelance_hub.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
import java.util.UUID;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.response.report.ReportProjectResponse;

@Component
public class ReportMapper {
    public record ProjectMetrics(UUID projectId, String projectName, UUID clientId,
            String clientName, String color, Integer targetMinutes,
            long trackedSeconds, long totalTasks, long completedTasks, ProjectStatus status) {}

    public ReportProjectResponse toProjectResponse(ProjectMetrics project) {
        Long targetSeconds = project.targetMinutes() == null
                ? null : project.targetMinutes().longValue() * 60;
        BigDecimal usagePercent = targetSeconds == null || targetSeconds == 0
                ? null : percentage(project.trackedSeconds(), targetSeconds);
        return new ReportProjectResponse(
                project.projectId(), project.projectName(), project.clientId(),
                project.clientName(), project.color(), targetSeconds,
                project.trackedSeconds(), usagePercent,
                percentage(project.completedTasks(), project.totalTasks()), project.status()
        );
    }

    private BigDecimal percentage(long part, long whole) {
        return whole == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }
}
