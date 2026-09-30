package th.ac.kku.freelance_hub.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.request.ChangeProjectStatusRequest;
import th.ac.kku.freelance_hub.dto.request.CreateProjectRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateProjectRequest;
import th.ac.kku.freelance_hub.dto.response.ProjectResponse;

import th.ac.kku.freelance_hub.dto.response.ProjectListItemResponse;

/** Operations on projects owned by the authenticated user. */
public interface ProjectService {

    ProjectResponse create(UUID ownerId, CreateProjectRequest request);

    ProjectListItemResponse getById(UUID ownerId, UUID projectId);

    ProjectStatusCounts countActiveAndCompleted(UUID ownerId);

    record ProjectStatusCounts(long activeCount, long completedCount) {
        public long totalCount() {
            return activeCount + completedCount;
        }
    }

    ProjectProgress getProgress(UUID ownerId, UUID projectId);

    record ProjectProgress(
            UUID projectId,
            Integer targetMinutes,
            long trackedSeconds,
            BigDecimal progressPercent,
            ProgressLevel level
    ) {}

    enum ProgressLevel {
        NO_TARGET,
        BELOW_80,
        REACHED_80,
        REACHED_100
    }

    default Page<ProjectListItemResponse> list(
            UUID ownerId,
            String search,
            ProjectStatus status,
            UUID clientId,
            Pageable pageable
    ) {
        return list(ownerId, search, status, clientId, pageable, false);
    }

    Page<ProjectListItemResponse> list(
            UUID ownerId,
            String search,
            ProjectStatus status,
            UUID clientId,
            Pageable pageable,
            boolean includeTasks
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
