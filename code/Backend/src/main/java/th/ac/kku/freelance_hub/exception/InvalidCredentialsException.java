package th.ac.kku.freelance_hub.exception;

/**
 * Exception thrown when user credentials are invalid
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("อีเมลหรือรหัสผ่านไม่ถูกต้อง");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
