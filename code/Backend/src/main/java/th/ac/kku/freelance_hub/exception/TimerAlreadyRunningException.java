package th.ac.kku.freelance_hub.exception;

/** Thrown when the current user attempts to start a second timer. */
public class TimerAlreadyRunningException extends RuntimeException {

    public TimerAlreadyRunningException() {
        super("A timer is already running");
    }
}
