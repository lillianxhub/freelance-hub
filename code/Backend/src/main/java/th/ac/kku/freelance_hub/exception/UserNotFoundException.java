package th.ac.kku.freelance_hub.exception;

import java.util.UUID;

/**
 * Exception thrown when user is not found
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("User not found with id: " + id);
    }

    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
    }
}
