package th.ac.kku.freelance_hub.mapper;

import org.springframework.stereotype.Component;

import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.dto.response.ProjectResponse;

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
}