package th.ac.kku.freelance_hub.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class InvalidStateException extends ApiException {
    public InvalidStateException(String message) { this(message, null); }
    public InvalidStateException(String message, Map<String, Object> details) {
        super(HttpStatus.CONFLICT, "INVALID_STATE", message, details);
    }
}
