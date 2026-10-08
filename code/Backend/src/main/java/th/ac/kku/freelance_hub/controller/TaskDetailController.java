package th.ac.kku.freelance_hub.controller;

import java.util.UUID;

import th.ac.kku.freelance_hub.common.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import th.ac.kku.freelance_hub.service.TaskService;
import th.ac.kku.freelance_hub.service.CurrentUserProvider;
import th.ac.kku.freelance_hub.dto.request.task.ChangeTaskStatusRequest;
import th.ac.kku.freelance_hub.dto.request.task.UpdateTaskRequest;
import th.ac.kku.freelance_hub.dto.response.task.TaskResponse;
@Tag(name = "Tasks", description = "Manage tasks within the authenticated user's projects")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TaskDetailController {

    private final TaskService taskService;
    private final CurrentUserProvider userService;

    @Operation(
            summary = "Get task detail",
            description = "Get a task by taskId from a project belonging to the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Task returned",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResult<TaskResponse>> getById(
            @PathVariable("taskId") UUID taskId
    ) {
        TaskResponse task = taskService.getById(
                userService.currentUserId(), taskId
        );

        return ResponseEntity.ok(
                ApiResult.success(
                        "ดึงข้อมูลงานย่อยสำเร็จ", task
                )
        );
    }

    @Operation(
            summary = "Update a task",
            description = "Update a task's name and description in an owned project that allows task editing"
    )
    @ApiResponse(responseCode = "200", description = "Task updated",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid task data")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "409", description = "Project status does not allow editing tasks",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResult<TaskResponse>> update(
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        TaskResponse task = taskService.update(
                userService.currentUserId(), taskId, request
        );

        return ResponseEntity.ok(
                ApiResult.success(
                        "แก้ไขงานย่อยสำเร็จ", task
                )
        );
    }

    @Operation(
            summary = "Change task status",
            description = "Move a task from OPEN to IN_PROGRESS before completing it. "
                    + "COMPLETED can return to IN_PROGRESS but not OPEN. "
                    + "Sending the current status makes no change. The project must allow task editing."
    )
    @ApiResponse(responseCode = "200", description = "Task status updated",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid task status")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "409", description = "Task status change is not allowed",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<ApiResult<TaskResponse>> changeStatus(
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody ChangeTaskStatusRequest request
    ) {
        TaskResponse task = taskService.changeStatus(
                userService.currentUserId(), taskId, request
        );

        return ResponseEntity.ok(
                ApiResult.success(
                        "เปลี่ยนสถานะงานย่อยสำเร็จ", task
                )
        );
    }

    @Operation(
            summary = "Delete a task",
            description = "Soft-delete a task by setting is_active to false and deleted_at to the deletion time. "
                    + "Time entries are preserved, and the owned project must allow task editing."
    )
    @ApiResponse(responseCode = "200", description = "Task deleted",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "409", description = "Project status does not allow deleting tasks",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResult<Void>> delete(
            @PathVariable("taskId") UUID taskId
    ) {
        taskService.delete(userService.currentUserId(), taskId);
        return ResponseEntity.ok(
                ApiResult.<Void>success(
                        "ลบงานย่อยสำเร็จ", null
                )
        );
    }
}
