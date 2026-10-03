package th.ac.kku.freelance_hub.mapper;

import org.springframework.stereotype.Component;

import th.ac.kku.freelance_hub.domain.entity.Project;

import java.math.BigDecimal;
import java.math.RoundingMode;
import th.ac.kku.freelance_hub.dto.response.project.ProjectListItemResponse;
import th.ac.kku.freelance_hub.dto.response.project.ProjectResponse;
@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .clientId(project.getClient().getId())
                .name(project.getName())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .color(project.getColor())
                .targetMinutes(project.getTargetMinutes())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .version(project.getVersion())
                .build();
    }

    public ProjectListItemResponse toListItemResponse(
            Project project,
            long totalTasks,
            long completedTasks
    ) {
        BigDecimal targetHours = project.getTargetMinutes() == null
                ? null
                : BigDecimal.valueOf(project.getTargetMinutes())
                        .divide(
                                BigDecimal.valueOf(60),
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal taskPercent = totalTasks == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(completedTasks)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(totalTasks),
                                2,
                                RoundingMode.HALF_UP
                        );

        ProjectListItemResponse.ClientSummary client =
                ProjectListItemResponse.ClientSummary.builder()
                        .id(project.getClient().getId())
                        .name(project.getClient().getName())
                        .build();

        ProjectListItemResponse.TaskProgress taskProgress =
                ProjectListItemResponse.TaskProgress.builder()
                        .totalTasks(totalTasks)
                        .completedTasks(completedTasks)
                        .percent(taskPercent)
                        .build();

        return ProjectListItemResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .color(project.getColor())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .targetHours(targetHours)
                .client(client)
                .taskProgress(taskProgress)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
