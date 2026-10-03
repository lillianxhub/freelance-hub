package th.ac.kku.freelance_hub.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.enums.EntryType;

import jakarta.persistence.LockModeType;

/**
 * Data access for time entries.
 *
 * <p>Queries exposed to the service layer include {@code ownerId} so time
 * entries cannot be read or changed across user accounts.</p>
 */
public interface TimeEntryRepository
        extends JpaRepository<TimeEntry, UUID>,
        JpaSpecificationExecutor<TimeEntry> {

    Optional<TimeEntry> findByIdAndOwnerIdAndIsActiveTrue(
            UUID id,
            UUID ownerId
    );

    boolean existsByIdAndOwnerIdAndIsActiveTrue(
            UUID id,
            UUID ownerId
    );

    boolean existsByOwnerIdAndEntryTypeAndEndedAtIsNullAndIsActiveTrue(
            UUID ownerId,
            EntryType entryType
    );

    Optional<TimeEntry> findByOwnerIdAndEntryTypeAndEndedAtIsNullAndIsActiveTrue(
            UUID ownerId,
            EntryType entryType
    );

    /**
     * Loads the current timer with a database write lock.
     *
     * <p>This method must be called inside a transaction when stopping or
     * cancelling a timer.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TimeEntry> findLockedByOwnerIdAndEntryTypeAndEndedAtIsNullAndIsActiveTrue(
            UUID ownerId,
            EntryType entryType
    );

    Page<TimeEntry> findAllByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Loads every unlocked entry for a project, including soft-deleted entries
     * and running timers. Requires a transaction; row locks last until it ends.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TimeEntry> findLockedByOwnerIdAndProjectIdAndLockedAtIsNull(
            UUID ownerId,
            UUID projectId
    );

    /**
     * Finds entries in a half-open UTC range: start inclusive, end exclusive.
     */
    Page<TimeEntry> findAllByOwnerIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            UUID ownerId,
            Instant rangeStart,
            Instant rangeEnd,
            Pageable pageable
    );

    Page<TimeEntry> findAllByOwnerIdAndProjectId(
            UUID ownerId,
            UUID projectId,
            Pageable pageable
    );

    Page<TimeEntry> findAllByOwnerIdAndTaskId(
            UUID ownerId,
            UUID taskId,
            Pageable pageable
    );

    /** Only the duration is fetched for a total. */
    interface DurationSecondsView {
        Long getDurationSeconds();
    }

    /** Start time is also fetched when building daily chart buckets. */
    interface StartedAtDurationView extends DurationSecondsView {
        Instant getStartedAt();
    }

    /** Includes related visibility fields needed by dashboard aggregates. */
    interface AllocationView extends StartedAtDurationView {
        ProjectView getProject();

        TaskView getTask();
    }

    interface ProjectView {
        UUID getId();

        Boolean getIsActive();
    }

    interface TaskView {
        Boolean getIsActive();
    }

    /** The caller supplies half-open instant boundaries for the desired days. */
    <T> List<T>
    findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            UUID ownerId,
            Instant fromInclusive,
            Instant toExclusive,
            Class<T> projectionType
    );

    interface ProjectTotalView {
    UUID getProjectId();
    Long getTotalSeconds();
}

    @Query("""
        SELECT t.project.id AS projectId,
               SUM(t.durationSeconds) AS totalSeconds
        FROM TimeEntry t
        LEFT JOIN t.task task
        WHERE t.owner.id = :ownerId
          AND t.isActive = true
          AND t.endedAt IS NOT NULL
          AND t.durationSeconds IS NOT NULL
          AND t.project.isActive = true
          AND t.project.deletedAt IS NULL
          AND (task IS NULL OR task.isActive = true)
        GROUP BY t.project.id
        """)
    List<ProjectTotalView> sumCompletedSecondsByProject(
            @Param("ownerId") UUID ownerId
);
}
