package th.ac.kku.freelance_hub.exception.handling;

import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import th.ac.kku.freelance_hub.exception.ApiException;
import th.ac.kku.freelance_hub.exception.LoginRateLimitedException;

@Component
public class ApiExceptionHandler implements ErrorHandler {
    public int getOrder() { return 100; }
    public Optional<ErrorDescriptor> handle(Exception exception, ErrorContext context) {
        if (context.source() == ErrorContext.Source.FILTER_DEPENDENCY || !(exception instanceof ApiException ex))
            return Optional.empty();
        var headers = new HttpHeaders();
        if (ex instanceof LoginRateLimitedException limited)
            headers.set(HttpHeaders.RETRY_AFTER, Long.toString(limited.getRetryAfterSeconds()));
        return Optional.of(new ErrorDescriptor(ex.getStatus(), ex.getCode(), ex.getMessage(), ex.getDetails(), null, headers));
    }
}
