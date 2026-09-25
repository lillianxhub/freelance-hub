package th.ac.kku.freelance_hub.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

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

    Optional<TimeEntry> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<TimeEntry> findByOwnerIdAndEntryTypeAndEndedAtIsNull(
            UUID ownerId,
            EntryType entryType
    );

    boolean existsByOwnerIdAndEntryTypeAndEndedAtIsNull(
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
    Optional<TimeEntry> findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
            UUID ownerId,
            EntryType entryType
    );

    Page<TimeEntry> findAllByOwnerId(UUID ownerId, Pageable pageable);

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
}
