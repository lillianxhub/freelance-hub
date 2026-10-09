package th.ac.kku.freelance_hub.common.response;

import th.ac.kku.freelance_hub.exception.handling.ErrorDescriptor;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.MDC;
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

    public ApiResult<Void> body(ErrorDescriptor descriptor) {
        ApiError error = new ApiError();
        error.setCode(descriptor.code());
        error.setDetails(descriptor.details());
        error.setStatus(descriptor.status().value());
        error.setTimestamp(Instant.now(clock));
        error.setFieldErrors(descriptor.fieldErrors());
        error.setTraceId(MDC.get(RequestTraceFilter.MDC_KEY));
        return ApiResult.error(descriptor.message(), error);
    }

    public ResponseEntity<ApiResult<Void>> response(ErrorDescriptor descriptor) {
        return ResponseEntity.status(descriptor.status()).headers(descriptor.headers()).body(body(descriptor));
    }
}
