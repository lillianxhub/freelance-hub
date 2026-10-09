package th.ac.kku.freelance_hub.exception.handling;

import java.util.Optional;
import org.springframework.core.Ordered;

public interface ErrorHandler extends Ordered {
    Optional<ErrorDescriptor> handle(Exception exception, ErrorContext context);
    default boolean isFallback() { return false; }
}
