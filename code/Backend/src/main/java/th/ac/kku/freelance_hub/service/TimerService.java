package th.ac.kku.freelance_hub.service;

import java.util.Optional;
import java.util.UUID;
import th.ac.kku.freelance_hub.dto.request.timeentry.StartTimerRequest;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryResponse;
/** Timer operations for the authenticated user. */
public interface TimerService {

    TimeEntryResponse startTimer(UUID ownerId, StartTimerRequest request);

    Optional<TimeEntryResponse> getCurrentTimer(UUID ownerId);

    TimeEntryResponse stopTimer(UUID ownerId);

    void cancelTimer(UUID ownerId);
}
