package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.UUID;
import java.util.List;

import th.ac.kku.freelance_hub.common.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.common.response.PaginationMeta;
import th.ac.kku.freelance_hub.exception.ErrorResponse;
import th.ac.kku.freelance_hub.service.ProjectService;
import th.ac.kku.freelance_hub.service.UserService;
import th.ac.kku.freelance_hub.dto.request.project.ChangeProjectStatusRequest;
import th.ac.kku.freelance_hub.dto.request.project.CreateProjectRequest;
import th.ac.kku.freelance_hub.dto.request.project.ProjectFilterRequest;
import th.ac.kku.freelance_hub.dto.request.project.UpdateProjectRequest;
import th.ac.kku.freelance_hub.dto.response.project.ProjectListItemResponse;
import th.ac.kku.freelance_hub.dto.response.project.ProjectResponse;
@Tag(name = "Projects", description = "Manage projects belonging to the authenticated user")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;

    //Create, Read, Update, Delete (CRUD) operations for projects
    @Operation(
            summary = "Create a project",
            description = "Create a project for the authenticated user using one of their clients"
    )
    @ApiResponse(responseCode = "201", description = "Project created",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid project data")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Client not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<ApiResult<ProjectResponse>> create(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.create(currentOwnerId(), request);

        return ResponseEntity.created(URI.create("/api/projects/" + response.getId()))
                .body(ApiResult.success(
                        "สร้างโปรเจกต์สำเร็จ", response));
    }

    @Operation(
            summary = "List projects",
            description = "ค้นหา Project จากชื่อ Project หรือ Client "
                    + "กรองสถานะ เรียงลำดับ และแบ่งหน้า; "
                    + "ส่ง include=tasks เมื่อต้องการรายการ Task ที่ยังใช้งานของแต่ละ Project; "
                    + "status=ALL จะแสดงทุกสถานะรวม ARCHIVED"
    )
    @ApiResponse(
            responseCode = "200",
            description = "ดึงรายการโปรเจกต์สำเร็จ",
            useReturnTypeSchema = true
    )
    @ApiResponse(
            responseCode = "400",
            description = "ข้อมูลค้นหา เรียงลำดับ หรือแบ่งหน้าไม่ถูกต้อง"
    )
    @ApiResponse(
            responseCode = "401",
            description = "ต้องเข้าสู่ระบบ"
    )
    @GetMapping
    public ResponseEntity<ApiResult<List<ProjectListItemResponse>>> list(
            @ParameterObject @Valid @ModelAttribute ProjectFilterRequest filter,
            @RequestParam(name = "clientId", required = false) UUID clientId
    ) {
        String sortField = switch (filter.getSortBy()) {
            case "update_at" -> "updatedAt";
            case "end_date" -> "endDate";
            case "project_name" -> "name";
            default -> throw new IllegalArgumentException(
                    "ฟิลด์ที่ใช้เรียงลำดับไม่ถูกต้อง"
            );
        };

        Sort.Order primaryOrder = new Sort.Order(
                filter.getDirection(),
                sortField
        );

        if ("name".equals(sortField)) {
            primaryOrder = primaryOrder.ignoreCase();
        }

        Sort sort = Sort.by(
                primaryOrder,
                Sort.Order.asc("id")
        );

        PageRequest pageable = PageRequest.of(
                filter.getPage() - 1,
                filter.getLimit(),
                sort
        );

        boolean allStatuses = "ALL".equals(filter.getStatus());
        ProjectStatus status = filter.getStatus() == null || allStatuses
                ? null : ProjectStatus.valueOf(filter.getStatus());
        Page<ProjectListItemResponse> projects = projectService.list(
                currentOwnerId(),
                filter.getSearch(),
                status,
                clientId,
                pageable,
                "tasks".equals(filter.getInclude()),
                allStatuses
        );

        PaginationMeta meta = PaginationMeta.builder()
                .page(projects.getNumber() + 1)
                .limit(projects.getSize())
                .total(projects.getTotalElements())
                .totalPages(projects.getTotalPages())
                .build();

        return ResponseEntity.ok(
                ApiResult.success(
                        "ดึงรายการโปรเจกต์สำเร็จ",
                        projects.getContent(),
                        meta
                )
        );
    }

    @Operation(
            summary = "Get project detail",
            description = "ดึงรายละเอียดโปรเจกต์ของผู้ใช้ พร้อมข้อมูล Client "
                    + "เวลาเป้าหมายเป็นชั่วโมง และความคืบหน้าของงานย่อยที่ยังใช้งาน"
    )
    @ApiResponse(responseCode = "200", description = "ดึงรายละเอียดโปรเจกต์สำเร็จ",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "ต้องเข้าสู่ระบบ")
    @ApiResponse(responseCode = "404", description = "ไม่พบโปรเจกต์",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<ProjectListItemResponse>> getById(
            @PathVariable("id") UUID id
    ) {
        ProjectListItemResponse project = projectService.getById(currentOwnerId(), id);
        return ResponseEntity.ok(
                ApiResult.success(
                        "ดึงรายละเอียดโปรเจกต์สำเร็จ", project
                )
        );
    }


    @Operation(
            summary = "Update a project",
            description = "แก้ไขรายละเอียดและ Client ของโปรเจกต์ที่ผู้ใช้เป็นเจ้าของ"
    )
    @ApiResponse(
            responseCode = "200",
            description = "แก้ไขโปรเจกต์สำเร็จ",
            useReturnTypeSchema = true
    )
    @ApiResponse(
            responseCode = "400",
            description = "ข้อมูลโปรเจกต์ไม่ถูกต้อง"
    )
    @ApiResponse(
            responseCode = "401",
            description = "ต้องเข้าสู่ระบบ"
    )
    @ApiResponse(
            responseCode = "404",
            description = "ไม่พบโปรเจกต์หรือ Client",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<ProjectResponse>> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        ProjectResponse response = projectService.update(
                currentOwnerId(),
                id,
                request
        );

        return ResponseEntity.ok(
                ApiResult.success(
                        "แก้ไขโปรเจกต์สำเร็จ",
                        response
                )
        );
    }

    @Operation(
            summary = "Change project status",
            description = "เปลี่ยนสถานะโปรเจกต์ตามกฎ State "
                    + "โดย ARCHIVED จะตั้ง is_active เป็น false"
    )
    @ApiResponse(
            responseCode = "200",
            description = "เปลี่ยนสถานะโปรเจกต์สำเร็จ",
            useReturnTypeSchema = true
    )
    @ApiResponse(
            responseCode = "400",
            description = "ข้อมูลสถานะไม่ถูกต้อง"
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
            description = "ไม่อนุญาตให้เปลี่ยนไปยังสถานะที่ระบุ",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResult<ProjectResponse>> changeStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ChangeProjectStatusRequest request
    ) {
        ProjectResponse response = projectService.changeStatus(
                currentOwnerId(),
                id,
                request
        );

        return ResponseEntity.ok(
                ApiResult.success(
                        "เปลี่ยนสถานะโปรเจกต์สำเร็จ",
                        response
                )
        );
    }

    @Operation(
            summary = "Delete a project",
            description = "ลบโปรเจกต์แบบ soft delete "
                    + "โดยตั้งสถานะ ARCHIVED, is_active=false และบันทึก deleted_at"
    )
    @ApiResponse(
            responseCode = "200",
            description = "ลบโปรเจกต์สำเร็จ",
            useReturnTypeSchema = true
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
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(
            @PathVariable("id") UUID id
    ) {
        projectService.archive(currentOwnerId(), id);

        return ResponseEntity.ok(
                ApiResult.<Void>success(
                        "ลบโปรเจกต์สำเร็จ",
                        null
                )
        );
    }

    private UUID currentOwnerId() {
        return userService.getCurrentUserEntity().getId();
    }
}
