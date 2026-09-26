package th.ac.kku.freelance_hub.exception;

import java.util.UUID;

/**
 * When can't find Project or Project does not belong to the current user.
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(UUID id) {
        super("Project not found with id: " + id);
    }
}