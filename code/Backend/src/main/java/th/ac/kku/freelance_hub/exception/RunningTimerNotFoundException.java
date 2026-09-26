package th.ac.kku.freelance_hub.exception;

/** Thrown when the current user has no running timer to stop or cancel. */
public class RunningTimerNotFoundException extends RuntimeException {

    public RunningTimerNotFoundException() {
        super("No running timer found");
    }
}
