package th.ac.kku.freelance_hub.exception;

import java.util.UUID;

/** Thrown when a locked time entry is edited or deleted. */
public class TimeEntryLockedException extends RuntimeException {

    public TimeEntryLockedException(UUID id) {
        super("Time entry is locked with id: " + id);
    }
}
