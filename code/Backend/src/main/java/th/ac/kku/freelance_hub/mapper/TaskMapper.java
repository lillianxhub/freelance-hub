package th.ac.kku.freelance_hub.mapper;

import org.springframework.stereotype.Component;

import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.dto.response.TaskResponse;

@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .name(task.getName())
                .description(task.getDescription())
                .status(task.getStatus())
                .sortOrder(task.getSortOrder())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .version(task.getVersion())
                .build();
    }
}


