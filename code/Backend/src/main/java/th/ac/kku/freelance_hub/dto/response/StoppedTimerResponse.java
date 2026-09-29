package th.ac.kku.freelance_hub.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Timer data returned after the current timer has been stopped. */
public record StoppedTimerResponse(
        UUID id,
        Instant startedAt,
        Instant endedAt,
        Long durationSeconds
) {

    public static StoppedTimerResponse from(TimeEntryResponse source) {
        return new StoppedTimerResponse(
                source.getId(),
                source.getStartedAt(),
                source.getEndedAt(),
                source.getDurationSeconds()
        );
    }
}
