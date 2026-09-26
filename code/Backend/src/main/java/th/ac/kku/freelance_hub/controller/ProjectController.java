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
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.request.ChangeProjectStatusRequest;
import th.ac.kku.freelance_hub.dto.request.CreateProjectRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateProjectRequest;
import th.ac.kku.freelance_hub.dto.response.ProjectResponse;
import th.ac.kku.freelance_hub.exception.ErrorResponse;
import th.ac.kku.freelance_hub.service.ProjectService;
import th.ac.kku.freelance_hub.service.UserService;

@Tag(name = "Projects", description = "Manage projects belonging to the authenticated user")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;

    @Operation(
            summary = "Create a project",
            description = "Create a project for the authenticated user using one of their clients"
    )
    @ApiResponse(responseCode = "201", description = "Project created",
            content = @Content(schema = @Schema(implementation = ProjectResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid project data")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Client not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request
    ) {
        ProjectResponse response = projectService.create(
                currentOwnerId(), request
        );
        return ResponseEntity
                .created(URI.create("/api/projects/" + response.getId()))
                .body(response);
    }

    @Operation(
            summary = "List projects",
            description = "List the authenticated user's projects with optional name search, status and client filters, sorting, and pagination"
    )
    @ApiResponse(responseCode = "200", description = "Page of projects returned")
    @ApiResponse(responseCode = "400", description = "Invalid search, sorting, or pagination options")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) ProjectStatus status,
            @RequestParam(name = "clientId", required = false) UUID clientId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(projectService.list(
                currentOwnerId(), search, status, clientId, pageable
        ));
    }

    @Operation(
            summary = "Get a project",
            description = "Get one project belonging to the authenticated user by ID"
    )
    @ApiResponse(responseCode = "200", description = "Project returned",
            content = @Content(schema = @Schema(implementation = ProjectResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(
                projectService.getById(currentOwnerId(), id)
        );
    }

    @Operation(
            summary = "Update a project",
            description = "Update the details or client of a project belonging to the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Project updated",
            content = @Content(schema = @Schema(implementation = ProjectResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid project data")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project or client not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ResponseEntity.ok(
                projectService.update(currentOwnerId(), id, request)
        );
    }

    @Operation(
            summary = "Change project status",
            description = "Change a project's status according to the allowed status transitions"
    )
    @ApiResponse(responseCode = "200", description = "Project status changed",
            content = @Content(schema = @Schema(implementation = ProjectResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid status request")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Status transition is not allowed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjectResponse> changeStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ChangeProjectStatusRequest request
    ) {
        return ResponseEntity.ok(
                projectService.changeStatus(currentOwnerId(), id, request)
        );
    }

    @Operation(
            summary = "Archive a project",
            description = "Set a project to ARCHIVED while preserving its data"
    )
    @ApiResponse(responseCode = "204", description = "Project archived", content = @Content)
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(
            @PathVariable("id") UUID id
    ) {
        projectService.archive(currentOwnerId(), id);
        return ResponseEntity.noContent().build();
    }

    private UUID currentOwnerId() {
        return userService.getCurrentUserEntity().getId();
    }
}