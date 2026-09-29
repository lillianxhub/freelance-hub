package th.ac.kku.freelance_hub.exception;

/**
 * Exception thrown when email already exists during registration
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("อีเมลนี้ถูกใช้งานแล้ว: " + email);
    }
}
