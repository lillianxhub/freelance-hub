package th.ac.kku.freelance_hub.exception;

import java.util.UUID;

/** Thrown when a time entry is missing or does not belong to the current user. */
public class TimeEntryNotFoundException extends RuntimeException {

    public TimeEntryNotFoundException(UUID id) {
        super("Time entry not found with id: " + id);
    }
}
