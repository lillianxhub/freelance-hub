package th.ac.kku.freelance_hub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;

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
}
