package th.ac.kku.freelance_hub.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class InvalidArgumentException extends ApiException {
    public InvalidArgumentException(String message) { this(message, null); }
    public InvalidArgumentException(String message, Map<String, Object> details) {
        super(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", message, details);
    }
}
