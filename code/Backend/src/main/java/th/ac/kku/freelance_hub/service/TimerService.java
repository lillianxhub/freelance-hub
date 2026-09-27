package th.ac.kku.freelance_hub.service;

import java.util.UUID;

import th.ac.kku.freelance_hub.dto.request.StartTimerRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;

/** Timer operations for the authenticated user. */
public interface TimerService {

    TimeEntryResponse startTimer(UUID ownerId, StartTimerRequest request);

    TimeEntryResponse getCurrentTimer(UUID ownerId);

    TimeEntryResponse stopTimer(UUID ownerId);

    void cancelTimer(UUID ownerId);
}
