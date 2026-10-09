package th.ac.kku.freelance_hub.exception.handling;

import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

/** Public error data, independent of transport and envelope construction. */
public record ErrorDescriptor(HttpStatusCode status, String code, String message,
        Map<String, Object> details, Map<String, String> fieldErrors, HttpHeaders headers) {
    public ErrorDescriptor {
        Objects.requireNonNull(status, "status");
        if (!status.isError()) throw new IllegalArgumentException("status must be an error status");
        if (code == null || code.isBlank() || message == null || message.isBlank())
            throw new IllegalArgumentException("code and message must not be blank");
        details = details == null ? null : Map.copyOf(details);
        fieldErrors = fieldErrors == null ? null : Map.copyOf(fieldErrors);
        HttpHeaders copy = new HttpHeaders();
        if (headers != null) headers.forEach((key, values) -> copy.put(key, java.util.List.copyOf(values)));
        headers = HttpHeaders.readOnlyHttpHeaders(copy);
    }
    public static ErrorDescriptor of(HttpStatusCode status, String code, String message) {
        return new ErrorDescriptor(status, code, message, null, null, null);
    }
}
