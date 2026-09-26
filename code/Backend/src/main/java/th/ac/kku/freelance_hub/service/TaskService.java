package th.ac.kku.freelance_hub.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import th.ac.kku.freelance_hub.dto.request.CreateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.ReorderTaskRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTaskRequest;
import th.ac.kku.freelance_hub.dto.response.TaskResponse;

public interface TaskService {

    TaskResponse create(UUID ownerId, UUID projectId, CreateTaskRequest request);

    Page<TaskResponse> list(UUID ownerId, UUID projectId, Pageable pageable);

    TaskResponse getById(UUID ownerId, UUID projectId, UUID taskId);

    TaskResponse update(
            UUID ownerId,
            UUID projectId,
            UUID taskId,
            UpdateTaskRequest request
    );

    TaskResponse start(UUID ownerId, UUID projectId, UUID taskId);

    TaskResponse complete(UUID ownerId, UUID projectId, UUID taskId);

    TaskResponse reorder(
            UUID ownerId,
            UUID projectId,
            UUID taskId,
            ReorderTaskRequest request
    );

    void delete(UUID ownerId, UUID projectId, UUID taskId);
}