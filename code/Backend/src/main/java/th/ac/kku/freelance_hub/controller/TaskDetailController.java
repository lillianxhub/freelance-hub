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
import th.ac.kku.freelance_hub.exception.ErrorResponse;
import th.ac.kku.freelance_hub.service.TaskService;
import th.ac.kku.freelance_hub.service.UserService;
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
    private final UserService userService;

    @Operation(
            summary = "Get task detail",
            description = "ดึงรายละเอียดงานย่อยด้วย taskId "
                    + "โดยผู้ใช้ต้องเป็นเจ้าของโปรเจกต์ของงานนั้น"
    )
    @ApiResponse(responseCode = "200", description = "ดึงข้อมูลงานย่อยสำเร็จ",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "ต้องเข้าสู่ระบบ")
    @ApiResponse(responseCode = "404", description = "ไม่พบงานย่อย",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResult<TaskResponse>> getById(
            @PathVariable("taskId") UUID taskId
    ) {
        TaskResponse task = taskService.getById(
                userService.getCurrentUserEntity().getId(), taskId
        );

        return ResponseEntity.ok(
                ApiResult.success(
                        "ดึงข้อมูลงานย่อยสำเร็จ", task
                )
        );
    }

    @Operation(
            summary = "Update a task",
            description = "แก้ชื่อและรายละเอียดงานย่อยด้วย taskId "
                    + "โดยผู้ใช้ต้องเป็นเจ้าของโปรเจกต์ "
                    + "และสถานะโปรเจกต์ต้องอนุญาตให้แก้ไขงานย่อย"
    )
    @ApiResponse(responseCode = "200", description = "แก้ไขงานย่อยสำเร็จ",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "ข้อมูลงานย่อยไม่ถูกต้อง")
    @ApiResponse(responseCode = "401", description = "ต้องเข้าสู่ระบบ")
    @ApiResponse(responseCode = "404", description = "ไม่พบงานย่อย",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "สถานะโปรเจกต์ไม่อนุญาตให้แก้ไขงานย่อย",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResult<TaskResponse>> update(
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        TaskResponse task = taskService.update(
                userService.getCurrentUserEntity().getId(), taskId, request
        );

        return ResponseEntity.ok(
                ApiResult.success(
                        "แก้ไขงานย่อยสำเร็จ", task
                )
        );
    }

    @Operation(
            summary = "Change task status",
            description = "เปลี่ยนสถานะ OPEN เป็น IN_PROGRESS หรือ COMPLETED "
                    + "และ IN_PROGRESS เป็น COMPLETED โดย COMPLETED ย้อนเป็น IN_PROGRESS ได้ แต่ย้อนเป็น OPEN ไม่ได้ "
                    + "การส่งสถานะเดิมจะไม่เปลี่ยนข้อมูล "
                    + "และสถานะโปรเจกต์ต้องอนุญาตให้แก้ไขงานย่อย"
    )
    @ApiResponse(responseCode = "200", description = "เปลี่ยนสถานะงานย่อยสำเร็จ",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "สถานะงานย่อยไม่ถูกต้อง")
    @ApiResponse(responseCode = "401", description = "ต้องเข้าสู่ระบบ")
    @ApiResponse(responseCode = "404", description = "ไม่พบงานย่อย",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "ไม่อนุญาตให้เปลี่ยนสถานะงานย่อย",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<ApiResult<TaskResponse>> changeStatus(
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody ChangeTaskStatusRequest request
    ) {
        TaskResponse task = taskService.changeStatus(
                userService.getCurrentUserEntity().getId(), taskId, request
        );

        return ResponseEntity.ok(
                ApiResult.success(
                        "เปลี่ยนสถานะงานย่อยสำเร็จ", task
                )
        );
    }

    @Operation(
            summary = "Delete a task",
            description = "ลบงานย่อยแบบ soft delete โดยตั้ง is_active=false "
                    + "และบันทึก deleted_at พร้อมเก็บข้อมูลเวลาเดิมไว้ "
                    + "ผู้ใช้ต้องเป็นเจ้าของโปรเจกต์ "
                    + "และสถานะโปรเจกต์ต้องอนุญาตให้แก้ไขงานย่อย"
    )
    @ApiResponse(responseCode = "200", description = "ลบงานย่อยสำเร็จ",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "ต้องเข้าสู่ระบบ")
    @ApiResponse(responseCode = "404", description = "ไม่พบงานย่อย",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "สถานะโปรเจกต์ไม่อนุญาตให้ลบงานย่อย",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResult<Void>> delete(
            @PathVariable("taskId") UUID taskId
    ) {
        taskService.delete(userService.getCurrentUserEntity().getId(), taskId);
        return ResponseEntity.ok(
                ApiResult.<Void>success(
                        "ลบงานย่อยสำเร็จ", null
                )
        );
    }
}
