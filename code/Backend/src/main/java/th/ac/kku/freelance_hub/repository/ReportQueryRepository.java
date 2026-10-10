package th.ac.kku.freelance_hub.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;

/**
 * Read-only report queries registered against TimeEntry as the primary entity.
 * Explicit queries also read related entities; the service calculates reports.
 */
@Transactional(readOnly = true)
public interface ReportQueryRepository extends Repository<TimeEntry, UUID> {

    @Query("""
            SELECT c FROM Client c
            WHERE c.owner.id = :ownerId AND c.deletedAt IS NULL
            """)
    List<Client> findClients(@Param("ownerId") UUID ownerId);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Client c
            WHERE c.id = :id AND c.owner.id = :ownerId AND c.deletedAt IS NULL
            """)
    boolean clientExists(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    @Query("""
            SELECT p FROM Project p JOIN FETCH p.client c
            WHERE p.owner.id = :ownerId AND p.deletedAt IS NULL AND c.deletedAt IS NULL
            """)
    List<Project> findProjects(@Param("ownerId") UUID ownerId);

    @Query("""
            SELECT p FROM Project p JOIN FETCH p.client c
            WHERE p.id = :id AND p.owner.id = :ownerId
              AND p.deletedAt IS NULL AND c.deletedAt IS NULL
            """)
    Optional<Project> findProject(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    @Query("""
            SELECT t FROM Task t JOIN FETCH t.project p
            WHERE p.owner.id = :ownerId AND p.id IN :projectIds
              AND t.isActive = true AND t.deletedAt IS NULL
            """)
    List<Task> findTasks(@Param("ownerId") UUID ownerId, @Param("projectIds") List<UUID> projectIds);

    @Query("""
            SELECT e FROM TimeEntry e JOIN FETCH e.project p
            WHERE e.owner.id = :ownerId AND p.id IN :projectIds
              AND e.isActive = true AND e.deletedAt IS NULL
              AND e.endedAt IS NOT NULL AND e.durationSeconds IS NOT NULL
            """)
    List<TimeEntry> findEntries(@Param("ownerId") UUID ownerId, @Param("projectIds") List<UUID> projectIds);

    @Query("""
            SELECT e FROM TimeEntry e JOIN FETCH e.project p
            WHERE e.owner.id = :ownerId AND p.id IN :projectIds
              AND e.isActive = true AND e.deletedAt IS NULL
              AND e.endedAt IS NOT NULL AND e.durationSeconds IS NOT NULL
              AND e.startedAt >= :fromInclusive AND e.startedAt < :toExclusive
            """)
    List<TimeEntry> findEntries(
            @Param("ownerId") UUID ownerId,
            @Param("projectIds") List<UUID> projectIds,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);
}
