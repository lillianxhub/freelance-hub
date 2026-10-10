package th.ac.kku.freelance_hub.exception.handling;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@Component
public class ValidationErrorHandler implements ErrorHandler {
    public int getOrder() { return 200; }
    public Optional<ErrorDescriptor> handle(Exception exception, ErrorContext context) {
        if (context.source() != ErrorContext.Source.MVC) return Optional.empty();
        Map<String, String> fields = new LinkedHashMap<>();
        if (exception instanceof MethodArgumentNotValidException ex) {
            ex.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), text(error.getDefaultMessage())));
        } else if (exception instanceof ConstraintViolationException ex) {
            ex.getConstraintViolations().forEach(error -> fields.putIfAbsent(error.getPropertyPath().toString(), text(error.getMessage())));
        } else if (exception instanceof HandlerMethodValidationException ex) {
            if (ex.isForReturnValue()) return Optional.empty();
            ex.getParameterValidationResults().forEach(result -> {
                String name = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(error -> fields.putIfAbsent(name == null ? "parameter" : name, text(error.getDefaultMessage())));
            });
        } else return Optional.empty();
        return Optional.of(new ErrorDescriptor(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "ข้อมูลที่ส่งมาไม่ถูกต้อง", null, fields, null));
    }
    private String text(String value) { return value == null ? "ข้อมูลไม่ถูกต้อง" : value; }
}
