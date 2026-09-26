package th.ac.kku.freelance_hub.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import th.ac.kku.freelance_hub.dto.request.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.StartTimerRequest;
import th.ac.kku.freelance_hub.dto.request.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.TimeEntrySummaryResponse;

/**
 * Business operations for time entries owned by the authenticated user.
 *
 * <p>The {@code ownerId} must come from authentication and must never be
 * accepted from request data.</p>
 */
public interface TimeEntryService {

    /** Starts the current user's only running timer. */
    TimeEntryResponse startTimer(UUID ownerId, StartTimerRequest request);

    /** Returns the current running timer so it can be resumed in the UI. */
    TimeEntryResponse getCurrentTimer(UUID ownerId);

    /** Stops the current running timer and calculates its duration. */
    TimeEntryResponse stopTimer(UUID ownerId);

    /** Cancels and removes the current running timer without recording it. */
    void cancelTimer(UUID ownerId);

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

    /** Lists the current user's time entries using optional filters. */
    Page<TimeEntryResponse> list(
            UUID ownerId,
            TimeEntryFilterRequest filter
    );

    /** Summarizes completed entries matching the requested filters. */
    TimeEntrySummaryResponse summarize(
            UUID ownerId,
            TimeEntryFilterRequest filter
    );
}
