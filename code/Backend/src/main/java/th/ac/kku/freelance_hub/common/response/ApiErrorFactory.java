package th.ac.kku.freelance_hub.common.response;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Builds the same error envelope for MVC and Spring Security responses. */
@Component
public class ApiErrorFactory {
    private final Clock clock;

    public ApiErrorFactory() {
        this(Clock.systemUTC());
    }

    ApiErrorFactory(Clock clock) {
        this.clock = clock;
    }

    public ApiResult<Void> body(HttpStatus status, String message, String code, Object details) {
        return body(status, message, code, details, null);
    }

    public ApiResult<Void> body(HttpStatus status, String message, String code,
                                Object details, Map<String, String> fieldErrors) {
        ApiError error = new ApiError();
        error.setCode(code);
        error.setDetails(details);
        error.setStatus(status.value());
        error.setTimestamp(Instant.now(clock));
        error.setFieldErrors(fieldErrors);
        error.setTraceId(MDC.get(RequestTraceFilter.MDC_KEY));
        return ApiResult.error(message, error);
    }

    public ResponseEntity<ApiResult<Void>> response(HttpStatus status, String message,
                                                     String code, Object details) {
        return ResponseEntity.status(status).body(body(status, message, code, details));
    }

    public ResponseEntity<ApiResult<Void>> validation(Map<String, String> fieldErrors) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body(status, "ข้อมูลที่ส่งมาไม่ถูกต้อง",
                "VALIDATION_ERROR", null, fieldErrors));
    }
}
