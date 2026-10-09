package th.ac.kku.freelance_hub.controller;

import th.ac.kku.freelance_hub.exception.InvalidArgumentException;
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
import th.ac.kku.freelance_hub.service.ProjectService;
import th.ac.kku.freelance_hub.service.CurrentUserProvider;
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
    private final CurrentUserProvider userService;

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
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @PostMapping
    public ResponseEntity<ApiResult<ProjectResponse>> create(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.create(currentOwnerId(), request);

        return ResponseEntity.created(URI.create("/api/projects/" + response.getId()))
                .body(ApiResult.success(
                        "สร้างโปรเจกต์สำเร็จ", response));
    }

    @Operation(
            summary = "List projects",
            description = "Search projects by project or client name, filter by status, sort, and paginate. "
                    + "Use include=tasks to include active tasks for each project. "
                    + "Use status=ALL to include every status, including ARCHIVED."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Page of projects returned",
            useReturnTypeSchema = true
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid search, sorting, or pagination options"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication required"
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
            default -> throw new InvalidArgumentException(
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
            description = "Get an owned project with its client, target hours, and active task progress"
    )
    @ApiResponse(responseCode = "200", description = "Project returned",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
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
            description = "Update the details and client of a project belonging to the authenticated user"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Project updated",
            useReturnTypeSchema = true
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid project data"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication required"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Project or client not found",
            content = @Content(
                    schema = @Schema(implementation = ApiResult.class)
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
            description = "Change a project status according to its allowed transitions. "
                    + "ARCHIVED sets is_active to false."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Project status updated",
            useReturnTypeSchema = true
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid project status"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication required"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(
                    schema = @Schema(implementation = ApiResult.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "Project status change is not allowed",
            content = @Content(
                    schema = @Schema(implementation = ApiResult.class)
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
            description = "Soft-delete a project by setting its status to ARCHIVED, "
                    + "is_active to false, and deleted_at to the deletion time"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Project deleted",
            useReturnTypeSchema = true
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication required"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(
                    schema = @Schema(implementation = ApiResult.class)
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
        return userService.currentUserId();
    }
}
