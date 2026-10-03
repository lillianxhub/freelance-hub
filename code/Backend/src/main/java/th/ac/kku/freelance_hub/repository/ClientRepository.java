package th.ac.kku.freelance_hub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for clients.
 *
 * <p>Every business-data query includes {@code ownerId} to prevent one user
 * from reading or modifying another user's clients.</p>
 */
public interface ClientRepository
    extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {

    Optional<Client> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Client> findAllByOwnerId(UUID ownerId, Pageable pageable);

    Page<Client> findAllByOwnerIdAndIsActive(
        UUID ownerId,
        Boolean isActive,
        Pageable pageable
    );

    /** Managed projects for the Client status cascade; soft-deleted projects are excluded. */
    @Query("""
            SELECT p
            FROM Project p
            WHERE p.owner.id = :ownerId
              AND p.client.id = :clientId
              AND p.deletedAt IS NULL
            ORDER BY p.id
            """)
    List<Project> findProjectsForClientStatusChange(
            @Param("ownerId") UUID ownerId, @Param("clientId") UUID clientId);

    /** Select only fields needed when a Client detail explicitly includes projects. */
    @Query("""
            SELECT p.id AS id, p.name AS name, p.color AS color,
                   p.status AS status, p.targetMinutes AS targetMinutes
            FROM Project p
            WHERE p.owner.id = :ownerId
              AND p.client.id = :clientId
              AND p.deletedAt IS NULL
            ORDER BY p.name, p.id
            """)
    List<IncludedProject> findIncludedProjects(
            @Param("ownerId") UUID ownerId, @Param("clientId") UUID clientId);

    /** Tasks are reached through their projects, never directly through Client. */
    @Query("""
            SELECT t.id AS id, t.project.id AS projectId,
                   t.name AS name, t.status AS status
            FROM Task t
            WHERE t.project.owner.id = :ownerId
              AND t.project.client.id = :clientId
              AND t.project.deletedAt IS NULL
              AND t.deletedAt IS NULL
            ORDER BY t.project.id, t.sortOrder, t.id
            """)
    List<IncludedTask> findIncludedTasks(
            @Param("ownerId") UUID ownerId, @Param("clientId") UUID clientId);

    /** Match time entry summary rules, including history on archived projects and tasks. */
    @Query("""
            SELECT p.client.id AS clientId, SUM(t.durationSeconds) AS totalSeconds
            FROM TimeEntry t
            JOIN t.project p
            WHERE t.owner.id = :ownerId
              AND p.client.id IN :clientIds
              AND t.isActive = true
              AND t.endedAt IS NOT NULL
              AND t.durationSeconds IS NOT NULL
            GROUP BY p.client.id
            """)
    List<ClientTrackedSeconds> sumTrackedSecondsByClientIds(
            @Param("ownerId") UUID ownerId, @Param("clientIds") List<UUID> clientIds);

    /** Aggregate completed, active time entries by their Project's Client. */
    @Query("""
            SELECT c.id AS clientId, c.name AS clientName,
                   SUM(t.durationSeconds) AS totalSeconds
            FROM TimeEntry t
            JOIN t.project p
            JOIN p.client c
            LEFT JOIN t.task task
            WHERE t.owner.id = :ownerId
              AND t.isActive = true
              AND t.deletedAt IS NULL
              AND p.isActive = true
              AND p.deletedAt IS NULL
              AND (task IS NULL OR (task.isActive = true AND task.deletedAt IS NULL))
              AND t.endedAt IS NOT NULL
              AND t.durationSeconds IS NOT NULL
              AND t.startedAt >= :fromInclusive
              AND t.startedAt < :toExclusive
            GROUP BY c.id, c.name
            ORDER BY SUM(t.durationSeconds) DESC, c.name ASC, c.id ASC
            """)
    List<ClientTimeTotal> sumCompletedTimeByClient(
            @Param("ownerId") UUID ownerId,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    interface IncludedProject {
        UUID getId();
        String getName();
        String getColor();
        ProjectStatus getStatus();
        Integer getTargetMinutes();
    }

    interface IncludedTask {
        UUID getId();
        UUID getProjectId();
        String getName();
        TaskStatus getStatus();
    }

    interface ClientTimeTotal {
        UUID getClientId();
        String getClientName();
        Long getTotalSeconds();
    }

    interface ClientTrackedSeconds {
        UUID getClientId();
        Long getTotalSeconds();
    }
}
