package th.ac.kku.freelance_hub.exception;

/**
 * Exception thrown when email already exists during registration
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email);
    }
}
