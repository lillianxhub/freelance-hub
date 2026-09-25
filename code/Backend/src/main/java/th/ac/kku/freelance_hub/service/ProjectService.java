package th.ac.kku.freelance_hub.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.request.ChangeProjectStatusRequest;
import th.ac.kku.freelance_hub.dto.request.CreateProjectRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateProjectRequest;
import th.ac.kku.freelance_hub.dto.response.ProjectResponse;

/** Operations on projects owned by the authenticated user. */
public interface ProjectService {

    ProjectResponse create(UUID ownerId, CreateProjectRequest request);

    ProjectResponse getById(UUID ownerId, UUID projectId);

    Page<ProjectResponse> list(
            UUID ownerId,
            String search,
            ProjectStatus status,
            UUID clientId,
            Pageable pageable
    );

    ProjectResponse update(
            UUID ownerId,
            UUID projectId,
            UpdateProjectRequest request
    );

    ProjectResponse changeStatus(
            UUID ownerId,
            UUID projectId,
            ChangeProjectStatusRequest request
    );

    void archive(UUID ownerId, UUID projectId);
}