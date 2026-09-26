package th.ac.kku.freelance_hub.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable snapshot published after a running timer has been stopped. */
public record TimerStoppedEvent(
        UUID timeEntryId,
        UUID ownerId,
        UUID projectId,
        UUID taskId,
        int durationMinutes,
        Instant startedAt,
        Instant endedAt
) {

    public TimerStoppedEvent {
        Objects.requireNonNull(timeEntryId, "timeEntryId is required");
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(projectId, "projectId is required");
        Objects.requireNonNull(startedAt, "startedAt is required");
        Objects.requireNonNull(endedAt, "endedAt is required");
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "durationMinutes must be greater than zero"
            );
        }
        if (!endedAt.isAfter(startedAt)) {
            throw new IllegalArgumentException(
                    "endedAt must be after startedAt"
            );
        }
    }
}
