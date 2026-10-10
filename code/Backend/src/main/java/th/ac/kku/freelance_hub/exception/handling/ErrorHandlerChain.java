package th.ac.kku.freelance_hub.exception.handling;

import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import org.springframework.stereotype.Component;

/** The first handler that accepts an exception terminates the chain. */
@Component
public class ErrorHandlerChain {
    private final List<ErrorHandler> handlers;
    public ErrorHandlerChain(List<ErrorHandler> handlers) {
        this.handlers = handlers.stream().sorted(Comparator.comparingInt(ErrorHandler::getOrder)).toList();
        var orders = new HashSet<Integer>();
        if (this.handlers.isEmpty() || this.handlers.stream().filter(ErrorHandler::isFallback).count() != 1
                || !this.handlers.get(this.handlers.size() - 1).isFallback())
            throw new IllegalStateException("Exactly one fallback must be last in the error chain");
        for (ErrorHandler handler : this.handlers)
            if (!orders.add(handler.getOrder())) throw new IllegalStateException("Duplicate error handler order");
    }
    public ErrorDescriptor resolve(Exception exception, ErrorContext context) {
        for (ErrorHandler handler : handlers) {
            var result = handler.handle(exception, context);
            if (result.isPresent()) return result.get();
        }
        throw new IllegalStateException("Fallback failed to handle an exception", exception);
    }
}
