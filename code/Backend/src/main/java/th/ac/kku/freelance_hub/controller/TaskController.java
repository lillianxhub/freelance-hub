package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import th.ac.kku.freelance_hub.common.response.ApiResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import th.ac.kku.freelance_hub.dto.request.CreateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.ReorderTaskRequest;
import th.ac.kku.freelance_hub.dto.request.ProjectTaskReorderRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTaskRequest;
import th.ac.kku.freelance_hub.common.response.PaginationMeta;
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
            description = "สร้างงานย่อยในโปรเจกต์ตามตำแหน่งที่ระบุ "
                    + "โดย sortOrder เริ่มจาก 0"
    )
    @ApiResponse(
            responseCode = "201",
            description = "สร้างงานย่อยสำเร็จ",
            useReturnTypeSchema = true
    )
    @ApiResponse(
            responseCode = "400",
            description = "ข้อมูลงานย่อยหรือลำดับไม่ถูกต้อง"
    )
    @ApiResponse(
            responseCode = "401",
            description = "ต้องเข้าสู่ระบบ"
    )
    @ApiResponse(
            responseCode = "404",
            description = "ไม่พบโปรเจกต์",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "สถานะโปรเจกต์ไม่อนุญาตให้สร้างงานย่อย",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @PostMapping
    public ResponseEntity<ApiResult<TaskResponse>> create(
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        TaskResponse response = taskService.create(
                currentOwnerId(),
                projectId,
                request
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/tasks/" + response.getId()
                ))
                .body(
                        ApiResult.success(
                                "สร้างงานย่อยสำเร็จ",
                                response
                        )
                );
    }

    @Operation(
            summary = "List tasks",
            description = "ดึงรายการงานย่อยในโปรเจกต์ กรองด้วย is_active "
                    + "โดยค่าเริ่มต้นเป็น true และรองรับ page, size, sort "
                    + "โดย page เริ่มจาก 0"
    )
    @ApiResponse(responseCode = "200", description = "ดึงรายการงานย่อยสำเร็จ",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "ข้อมูลตัวกรองหรือแบ่งหน้าไม่ถูกต้อง")
    @ApiResponse(responseCode = "401", description = "ต้องเข้าสู่ระบบ")
    @ApiResponse(responseCode = "404", description = "ไม่พบโปรเจกต์",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<ApiResult<List<TaskResponse>>> list(
            @PathVariable("projectId") UUID projectId,
            @RequestParam(name = "is_active", defaultValue = "true") boolean isActive,
            Pageable pageable
    ) {
        Page<TaskResponse> tasks = taskService.list(
                currentOwnerId(), projectId, isActive, pageable
        );

        PaginationMeta meta = PaginationMeta.builder()
                .page(tasks.getNumber() + 1)
                .limit(tasks.getSize())
                .total(tasks.getTotalElements())
                .totalPages(tasks.getTotalPages())
                .build();

        return ResponseEntity.ok(
                ApiResult.success(
                        "ดึงรายการงานย่อยสำเร็จ",
                        tasks.getContent(),
                        meta
                )
        );
    }

    // @Operation(
    //         summary = "Get a task",
    //         description = "Get one task from a project belonging to the authenticated user"
    // )
    // @ApiResponse(responseCode = "200", description = "Task returned",
    //         content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    // @ApiResponse(responseCode = "401", description = "Authentication required")
    // @ApiResponse(responseCode = "404", description = "Task not found",
    //         content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    // // Legacy route unused by Frontend; use GET /api/tasks/{taskId}.
    // // @GetMapping("/{taskId}")
    // public ResponseEntity<TaskResponse> getById(
    //         @PathVariable("projectId") UUID projectId,
    //         @PathVariable("taskId") UUID taskId
    // ) {
    //     return ResponseEntity.ok(
    //             taskService.getById(currentOwnerId(), projectId, taskId)
    //     );
    // }

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

    // @Operation(
    //         summary = "Start a task",
    //         description = "Change a task from OPEN to IN_PROGRESS"
    // )
    // @ApiResponse(responseCode = "200", description = "Task started",
    //         content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    // @ApiResponse(responseCode = "401", description = "Authentication required")
    // @ApiResponse(responseCode = "404", description = "Project or task not found",
    //         content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    // @ApiResponse(responseCode = "409", description = "Task cannot be started in its current state",
    //         content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    // // Legacy route unused by Frontend; use PATCH /api/tasks/{taskId}/status.
    // // @PatchMapping("/{taskId}/start")
    // public ResponseEntity<TaskResponse> start(
    //         @PathVariable("projectId") UUID projectId,
    //         @PathVariable("taskId") UUID taskId
    // ) {
    //     return ResponseEntity.ok(
    //             taskService.start(currentOwnerId(), projectId, taskId)
    //     );
    // }

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
            description = "ลบงานย่อยแบบ soft delete และปรับลำดับงานที่ยังใช้งาน โดยเก็บข้อมูลเวลาเดิมไว้"
    )
    @ApiResponse(responseCode = "204", description = "Task deleted", content = @Content)
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project or task not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "สถานะโปรเจกต์ไม่อนุญาตให้ลบงานย่อย",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("taskId") UUID taskId
    ) {
        taskService.delete(currentOwnerId(), projectId, taskId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reorder a task within a project",
            description = "ส่ง taskId และ sortOrder เพื่อย้ายงานย่อยไปยังตำแหน่งใหม่ "
                    + "โดยตำแหน่งเริ่มจาก 0 และนับเฉพาะงานที่ยังใช้งาน"
    )
    @ApiResponse(responseCode = "200", description = "เรียงลำดับงานย่อยสำเร็จ",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "ข้อมูลงานย่อยหรือลำดับไม่ถูกต้อง")
    @ApiResponse(responseCode = "401", description = "ต้องเข้าสู่ระบบ")
    @ApiResponse(responseCode = "404", description = "ไม่พบโปรเจกต์หรืองานย่อย",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "สถานะโปรเจกต์ไม่อนุญาตให้เรียงลำดับงาน",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/reorder")
    public ResponseEntity<ApiResult<TaskResponse>> reorderInProject(
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody ProjectTaskReorderRequest request
    ) {
        TaskResponse response = taskService.reorder(
                currentOwnerId(),
                projectId,
                request.getTaskId(),
                ReorderTaskRequest.builder().sortOrder(request.getSortOrder()).build()
        );

        return ResponseEntity.ok(
                ApiResult.success(
                        "เรียงลำดับงานย่อยสำเร็จ", response
                )
        );
    }
    private UUID currentOwnerId() {
        return userService.getCurrentUserEntity().getId();
    }
}