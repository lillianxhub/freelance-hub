package th.ac.kku.freelance_hub.exception.handling;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UnknownErrorHandler implements ErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(UnknownErrorHandler.class);
    public int getOrder() { return 500; }
    public boolean isFallback() { return true; }
    public Optional<ErrorDescriptor> handle(Exception exception, ErrorContext context) {
        log.error("Unhandled error at {} {}", context.source(), context.path(), exception);
        return Optional.of(ErrorDescriptor.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "เกิดข้อผิดพลาดภายในระบบ"));
    }
}
