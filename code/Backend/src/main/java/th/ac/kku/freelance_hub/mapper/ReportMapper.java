package th.ac.kku.freelance_hub.mapper;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import java.util.UUID;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.response.report.ReportProjectResponse;

@Component
public class ReportMapper {
    public record ProjectMetrics(UUID projectId, String projectName, UUID clientId,
            String clientName, String color, Long targetSeconds,
            long trackedSeconds, BigDecimal usagePercent,
            BigDecimal taskProgressPercent, ProjectStatus status) {}

    public ReportProjectResponse toProjectResponse(ProjectMetrics project) {
        return new ReportProjectResponse(
                project.projectId(), project.projectName(), project.clientId(),
                project.clientName(), project.color(), project.targetSeconds(),
                project.trackedSeconds(), project.usagePercent(),
                project.taskProgressPercent(), project.status()
        );
    }

}
