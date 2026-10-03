package th.ac.kku.freelance_hub.service;

import java.util.UUID;
import th.ac.kku.freelance_hub.dto.request.timeentry.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryResponse;
/**
 * Business operations for time entries owned by the authenticated user.
 *
 * <p>The {@code ownerId} must come from authentication and must never be
 * accepted from request data.</p>
 */
public interface TimeEntryService {

    /** Creates a completed time entry from an explicit range or duration. */
    TimeEntryResponse createManual(
            UUID ownerId,
            ManualTimeEntryRequest request
    );

    /** Updates an unlocked time entry owned by the current user. */
    TimeEntryResponse update(
            UUID ownerId,
            UUID entryId,
            UpdateTimeEntryRequest request
    );

    /** Deletes an unlocked completed time entry owned by the current user. */
    void delete(UUID ownerId, UUID entryId);

    /**
     * Permanently locks all unlocked entries of a project, including
     * soft-deleted entries, preserving existing lock timestamps.
     *
     * <p>The caller must verify ownership and the transition to COMPLETED,
     * and call this inside the same transaction as that transition.
     * A running timer causes an IllegalStateException; timers are not stopped
     * automatically.</p>
     *
     * <p>The caller must also prevent concurrent entry creation or reassignment
     * into the project while completing it; row locks only cover existing
     * entries.</p>
     */
    void lockByProject(UUID ownerId, UUID projectId);

}
