package th.ac.kku.freelance_hub.service;

import java.util.UUID;

import th.ac.kku.freelance_hub.dto.request.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;

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

}
