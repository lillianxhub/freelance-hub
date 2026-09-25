package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import th.ac.kku.freelance_hub.dto.request.CreateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.ReorderTaskRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTaskRequest;
import th.ac.kku.freelance_hub.dto.response.TaskResponse;
import th.ac.kku.freelance_hub.service.TaskService;
import th.ac.kku.freelance_hub.service.UserService;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TaskController {

    private final TaskService taskService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        TaskResponse response = taskService.create(
                currentOwnerId(), projectId, request
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/projects/" + projectId
                                + "/tasks/" + response.getId()
                ))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> list(
            @PathVariable("projectId") UUID projectId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                taskService.list(currentOwnerId(), projectId, pageable)
        );
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getById(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
    ) {
        return ResponseEntity.ok(
                taskService.getById(currentOwnerId(), projectId, taskId)
        );
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return ResponseEntity.ok(
                taskService.update(
                        currentOwnerId(), projectId, taskId, request
                )
        );
    }

    @PatchMapping("/{taskId}/start")
    public ResponseEntity<TaskResponse> start(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
    ) {
        return ResponseEntity.ok(
                taskService.start(currentOwnerId(), projectId, taskId)
        );
    }

    @PatchMapping("/{taskId}/complete")
    public ResponseEntity<TaskResponse> complete(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
    ) {
        return ResponseEntity.ok(
                taskService.complete(currentOwnerId(), projectId, taskId)
        );
    }

    @PatchMapping("/{taskId}/reorder")
    public ResponseEntity<TaskResponse> reorder(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody ReorderTaskRequest request
    ) {
        return ResponseEntity.ok(
                taskService.reorder(
                        currentOwnerId(), projectId, taskId, request
                )
        );
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
    ) {
        taskService.delete(currentOwnerId(), projectId, taskId);
        return ResponseEntity.noContent().build();
    }

    private UUID currentOwnerId() {
        return userService.getCurrentUserEntity().getId();
    }
    
}