package th.ac.kku.freelance_hub.exception;

import java.util.UUID;


/**
 * When can't find Task or Task not in Project user.
 */

public class TaskNotFoundException extends RuntimeException{

    public TaskNotFoundException(UUID id) {
        super("Task not found with id: " + id);
    }
}
