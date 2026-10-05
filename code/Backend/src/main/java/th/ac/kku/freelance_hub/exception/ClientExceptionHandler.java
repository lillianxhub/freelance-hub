package th.ac.kku.freelance_hub.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.common.response.ApiErrorFactory;
import th.ac.kku.freelance_hub.controller.ClientController;

/** Keeps Client errors in the same response envelope as Client successes. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ClientController.class)
public class ClientExceptionHandler {
    private final ApiErrorFactory errorFactory;

    public ClientExceptionHandler(ApiErrorFactory errorFactory) {
        this.errorFactory = errorFactory;
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> clientNotFound(ClientNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), "CLIENT_NOT_FOUND", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return errorFactory.validation(details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> unreadableBody(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "ข้อมูลที่ส่งมาไม่ถูกต้อง", "INVALID_REQUEST_BODY", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> invalidParameter(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "พารามิเตอร์ไม่ถูกต้อง", "INVALID_ARGUMENT", Map.of("field", ex.getName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> invalidArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_ARGUMENT", null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResult<Void>> invalidState(IllegalStateException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), "INVALID_STATE", null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResult<Void>> responseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        return error(status, message, status.name(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> unexpected(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในระบบ", "INTERNAL_SERVER_ERROR", null);
    }

    private ResponseEntity<ApiResult<Void>> error(HttpStatus status, String message, String code, Object details) {
        return errorFactory.response(status, message, code, details);
    }
}
