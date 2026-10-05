package th.ac.kku.freelance_hub.repository;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;

@Repository
public class ReportQueryRepository {
    private final EntityManager entityManager;

    public ReportQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public record Totals(long trackedSeconds, long entryCount) {}
    public record ProjectTime(UUID projectId, long trackedSeconds) {}
    public record TaskProgress(
            UUID projectId,
            long totalTasks,
            long completedTasks
    ) {}
    public record DailyTime(LocalDate date, long trackedSeconds) {}
    public record HourlyTime(int hour, long trackedSeconds) {}

    public List<Client> findVisibleClients(UUID ownerId) {
        return entityManager.createQuery("""
                SELECT c FROM Client c
                WHERE c.owner.id = :ownerId
                  AND c.deletedAt IS NULL
                ORDER BY c.name, c.id
                """, Client.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    public List<Project> findVisibleProjects(UUID ownerId) {
        return entityManager.createQuery("""
                SELECT p FROM Project p JOIN FETCH p.client c
                WHERE p.owner.id = :ownerId
                  AND p.deletedAt IS NULL
                  AND c.deletedAt IS NULL
                ORDER BY p.name, p.id
                """, Project.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    public Totals totals(
            UUID ownerId,
            Instant from,
            Instant toExclusive,
            UUID clientId,
            UUID projectId
    ) {
        Object[] row = entryQuery(
                "SELECT COALESCE(SUM(t.durationSeconds), 0), COUNT(t)",
                "",
                ownerId, from, toExclusive, clientId, projectId
        ).getSingleResult();

        return new Totals(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue()
        );
    }

    public List<ProjectTime> timeByProject(
            UUID ownerId,
            Instant from,
            Instant toExclusive,
            UUID clientId,
            UUID projectId
    ) {
        return entryQuery(
                "SELECT p.id, SUM(t.durationSeconds)",
                " GROUP BY p.id",
                ownerId, from, toExclusive, clientId, projectId
        ).getResultList().stream()
                .map(row -> new ProjectTime(
                        (UUID) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    public List<TaskProgress> taskProgress(UUID ownerId) {
        List<Object[]> rows = entityManager.createQuery("""
                SELECT t.project.id, COUNT(t),
                       SUM(CASE WHEN t.status = :done THEN 1 ELSE 0 END)
                FROM Task t
                WHERE t.project.owner.id = :ownerId
                  AND t.project.deletedAt IS NULL
                  AND t.isActive = true
                  AND t.deletedAt IS NULL
                GROUP BY t.project.id
                """, Object[].class)
                .setParameter("ownerId", ownerId)
                .setParameter("done", TaskStatus.COMPLETED)
                .getResultList();

        return rows.stream()
                .map(row -> new TaskProgress(
                        (UUID) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }

    public List<DailyTime> timeByDay(
            UUID ownerId,
            Instant from,
            Instant toExclusive,
            UUID clientId,
            UUID projectId
    ) {
        String select = """
                CAST(t.started_at AT TIME ZONE 'Asia/Bangkok' AS date),
                COALESCE(SUM(t.duration_seconds), 0)
                """;

        List<Object[]> rows = nativeTimeQuery(
                select,
                " GROUP BY 1 ORDER BY 1",
                ownerId, from, toExclusive, clientId, projectId
        ).getResultList();

        return rows.stream()
                .map(row -> new DailyTime(
                        row[0] instanceof LocalDate date
                                ? date
                                : ((Date) row[0]).toLocalDate(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    public List<HourlyTime> timeByHour(
            UUID ownerId,
            Instant from,
            Instant toExclusive,
            UUID clientId,
            UUID projectId
    ) {
        String select = """
                EXTRACT(HOUR FROM t.started_at AT TIME ZONE 'Asia/Bangkok'),
                COALESCE(SUM(t.duration_seconds), 0)
                """;

        List<Object[]> rows = nativeTimeQuery(
                select,
                " GROUP BY 1 ORDER BY 1",
                ownerId, from, toExclusive, clientId, projectId
        ).getResultList();

        return rows.stream()
                .map(row -> new HourlyTime(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    private TypedQuery<Object[]> entryQuery(
            String select,
            String suffix,
            UUID ownerId,
            Instant from,
            Instant toExclusive,
            UUID clientId,
            UUID projectId
    ) {
        String jpql = select + """
                 FROM TimeEntry t JOIN t.project p JOIN p.client c
                 WHERE t.owner.id = :ownerId
                   AND t.isActive = true
                   AND t.deletedAt IS NULL
                   AND t.endedAt IS NOT NULL
                   AND t.durationSeconds IS NOT NULL
                   AND p.deletedAt IS NULL
                   AND c.deletedAt IS NULL
                """;

        if (from != null) jpql += " AND t.startedAt >= :from";
        if (toExclusive != null) jpql += " AND t.startedAt < :to";
        if (clientId != null) jpql += " AND c.id = :clientId";
        if (projectId != null) jpql += " AND p.id = :projectId";

        TypedQuery<Object[]> query = entityManager
                .createQuery(jpql + suffix, Object[].class)
                .setParameter("ownerId", ownerId);

        if (from != null) query.setParameter("from", from);
        if (toExclusive != null) query.setParameter("to", toExclusive);
        if (clientId != null) query.setParameter("clientId", clientId);
        if (projectId != null) query.setParameter("projectId", projectId);
        return query;
    }

    @SuppressWarnings("unchecked")
    private Query nativeTimeQuery(
            String select,
            String suffix,
            UUID ownerId,
            Instant from,
            Instant toExclusive,
            UUID clientId,
            UUID projectId
    ) {
        String sql = "SELECT " + select + """
                 FROM time_entries t
                 JOIN projects p ON p.id = t.project_id
                 JOIN clients c ON c.id = p.client_id
                 WHERE t.owner_id = :ownerId
                   AND t.is_active = true
                   AND t.deleted_at IS NULL
                   AND t.ended_at IS NOT NULL
                   AND t.duration_seconds IS NOT NULL
                   AND p.deleted_at IS NULL
                   AND c.deleted_at IS NULL
                """;

        if (from != null) sql += " AND t.started_at >= :from";
        if (toExclusive != null) sql += " AND t.started_at < :to";
        if (clientId != null) sql += " AND c.id = :clientId";
        if (projectId != null) sql += " AND p.id = :projectId";

        Query query = entityManager.createNativeQuery(sql + suffix)
                .setParameter("ownerId", ownerId);

        if (from != null) query.setParameter("from", from);
        if (toExclusive != null) query.setParameter("to", toExclusive);
        if (clientId != null) query.setParameter("clientId", clientId);
        if (projectId != null) query.setParameter("projectId", projectId);
        return query;
    }
}
