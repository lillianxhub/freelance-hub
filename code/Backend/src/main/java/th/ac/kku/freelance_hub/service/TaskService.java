package th.ac.kku.freelance_hub.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import th.ac.kku.freelance_hub.dto.request.task.ChangeTaskStatusRequest;
import th.ac.kku.freelance_hub.dto.request.task.CreateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.task.ReorderTaskRequest;
import th.ac.kku.freelance_hub.dto.request.task.UpdateTaskRequest;
import th.ac.kku.freelance_hub.dto.response.task.TaskResponse;
public interface TaskService {

    TaskResponse create(UUID ownerId, UUID projectId, CreateTaskRequest request);

    Page<TaskResponse> list(
            UUID ownerId,
            UUID projectId,
            boolean isActive,
            Pageable pageable
    );

    TaskResponse getById(UUID ownerId, UUID taskId);

    TaskResponse getById(UUID ownerId, UUID projectId, UUID taskId);

    /** Empty when there is no time entry or its most recent entry has no task. */
    Optional<String> getLatestTimeEntryTaskName(UUID ownerId);

    TaskResponse update(UUID ownerId, UUID taskId, UpdateTaskRequest request);

    TaskResponse update(
            UUID ownerId,
            UUID projectId,
            UUID taskId,
            UpdateTaskRequest request
    );

    TaskResponse changeStatus(UUID ownerId, UUID taskId, ChangeTaskStatusRequest request);

    TaskResponse start(UUID ownerId, UUID projectId, UUID taskId);

    TaskResponse complete(UUID ownerId, UUID projectId, UUID taskId);

    TaskResponse reorder(
            UUID ownerId,
            UUID projectId,
            UUID taskId,
            ReorderTaskRequest request
    );

    void delete(UUID ownerId, UUID taskId);

    void delete(UUID ownerId, UUID projectId, UUID taskId);
}
