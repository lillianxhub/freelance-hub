package th.ac.kku.freelance_hub.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query("""
            SELECT t FROM Task t
            WHERE t.id = :id
                AND t.project.owner.id = :ownerId
                AND t.project.deletedAt IS NULL
                AND t.deletedAt IS NULL
            """)
    Optional<Task> findByIdAndProjectOwnerIdAndProjectDeletedAtIsNull(
            @Param("id") UUID id,
            @Param("ownerId") UUID ownerId
    );

    @Query("""
            SELECT t FROM Task t
            WHERE t.id = :id
                AND t.project.id = :projectId
                AND t.project.owner.id = :ownerId
                AND t.project.deletedAt IS NULL
                AND t.deletedAt IS NULL
            """)
    Optional<Task> findByIdAndProjectIdAndProjectOwnerId(
            @Param("id") UUID id,
            @Param("projectId") UUID projectId,
            @Param("ownerId") UUID ownerId
    );

    Page<Task> findAllByProjectIdAndProjectOwnerId(
            UUID projectId,
            UUID ownerId,
            Pageable pageable
    );

    Page<Task> findAllByProjectIdAndProjectOwnerIdAndIsActive(
            UUID projectId,
            UUID ownerId,
            boolean isActive,
            Pageable pageable
    );

    List<Task> findAllByProjectIdAndProjectOwnerIdOrderBySortOrderAsc(
            UUID projectId,
            UUID ownerId
    );

    long countByProjectId(
            UUID projectId
    );

    @Query("""
            SELECT
                t.project.id AS projectId,
                COUNT(t) AS totalTasks,
                SUM(
                    CASE
                        WHEN t.status = :completedStatus THEN 1
                        ELSE 0
                    END
                ) AS completedTasks
            FROM Task t
            WHERE t.project.owner.id = :ownerId
                AND t.project.id IN :projectIds
                AND t.isActive = true
            GROUP BY t.project.id
            """)
    List<TaskProgressSummary> summarizeProgressByProjectIds(
            @Param("ownerId") UUID ownerId,
            @Param("projectIds") List<UUID> projectIds,
            @Param("completedStatus") TaskStatus completedStatus
    );

    interface TaskProgressSummary {

        UUID getProjectId();

        long getTotalTasks();

        long getCompletedTasks();
    }
}