package th.ac.kku.freelance_hub.exception;

import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;

/** An expected API failure; status and public metadata belong to the exception. */
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    protected ApiException(HttpStatus status, String code, String message, Map<String, Object> details) {
        this(status, code, message, details, null);
    }

    protected ApiException(HttpStatus status, String code, String message, Map<String, Object> details, Throwable cause) {
        super(requireText(message, "message"), cause);
        this.status = Objects.requireNonNull(status, "status");
        if (!status.isError()) throw new IllegalArgumentException("status must be an error status");
        this.code = requireText(code, "code");
        if (!code.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("code must be UPPER_SNAKE_CASE");
        this.details = details == null ? null : Map.copyOf(details);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public Map<String, Object> getDetails() { return details; }
}
