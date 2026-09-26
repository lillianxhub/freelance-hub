package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.UUID;

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
import th.ac.kku.freelance_hub.service.ProjectService;
import th.ac.kku.freelance_hub.service.UserService;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;

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

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(
                projectService.getById(currentOwnerId(), id)
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ResponseEntity.ok(
                projectService.update(currentOwnerId(), id, request)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjectResponse> changeStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ChangeProjectStatusRequest request
    ) {
        return ResponseEntity.ok(
                projectService.changeStatus(currentOwnerId(), id, request)
        );
    }

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