package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import th.ac.kku.freelance_hub.exception.ErrorResponse;
import th.ac.kku.freelance_hub.service.TaskService;
import th.ac.kku.freelance_hub.service.UserService;

@Tag(name = "Tasks", description = "Manage tasks within the authenticated user's projects")
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TaskController {

    private final TaskService taskService;
    private final UserService userService;

    @Operation(
            summary = "Create a task",
            description = "Create a task at the specified zero-based position in a project"
    )
    @ApiResponse(responseCode = "201", description = "Task created",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid task data or sort order")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Project is completed or archived",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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

    @Operation(
            summary = "List tasks",
            description = "List tasks in a project with sorting and pagination"
    )
    @ApiResponse(responseCode = "200", description = "Page of tasks returned")
    @ApiResponse(responseCode = "400", description = "Invalid sorting or pagination options")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> list(
            @PathVariable("projectId") UUID projectId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                taskService.list(currentOwnerId(), projectId, pageable)
        );
    }

    @Operation(
            summary = "Get a task",
            description = "Get one task from a project belonging to the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Task returned",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getById(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
    ) {
        return ResponseEntity.ok(
                taskService.getById(currentOwnerId(), projectId, taskId)
        );
    }

    @Operation(
            summary = "Update a task",
            description = "Update a task's name and description"
    )
    @ApiResponse(responseCode = "200", description = "Task updated",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid task data")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project or task not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Project is completed or archived",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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

    @Operation(
            summary = "Start a task",
            description = "Change a task from OPEN to IN_PROGRESS"
    )
    @ApiResponse(responseCode = "200", description = "Task started",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project or task not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Task cannot be started in its current state",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{taskId}/start")
    public ResponseEntity<TaskResponse> start(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
    ) {
        return ResponseEntity.ok(
                taskService.start(currentOwnerId(), projectId, taskId)
        );
    }

    @Operation(
            summary = "Complete a task",
            description = "Mark a task as COMPLETED and record its completion time"
    )
    @ApiResponse(responseCode = "200", description = "Task completed",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project or task not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Task cannot be completed in its current state",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{taskId}/complete")
    public ResponseEntity<TaskResponse> complete(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
    ) {
        return ResponseEntity.ok(
                taskService.complete(currentOwnerId(), projectId, taskId)
        );
    }

    @Operation(
            summary = "Reorder a task",
            description = "Move a task to a new zero-based position within its project"
    )
    @ApiResponse(responseCode = "200", description = "Task reordered",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid sort order")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project or task not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Project is completed or archived",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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

    @Operation(
            summary = "Delete a task",
            description = "Delete a task and update the order of remaining tasks; tasks with time entries cannot be deleted"
    )
    @ApiResponse(responseCode = "204", description = "Task deleted", content = @Content)
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project or task not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Project is completed or archived, or the task has time entries",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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