package th.ac.kku.freelance_hub.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import th.ac.kku.freelance_hub.common.response.ApiErrorFactory;
import th.ac.kku.freelance_hub.common.response.ApiResult;

/** Fallback mappings for endpoints without a feature-specific exception handler. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final ApiErrorFactory errors;

    public GlobalExceptionHandler(ApiErrorFactory errors) {
        this.errors = errors;
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResult<Void>> emailAlreadyExists(EmailAlreadyExistsException ex) {
        return errors.response(HttpStatus.CONFLICT, ex.getMessage(), "EMAIL_ALREADY_EXISTS", null);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> userNotFound(UserNotFoundException ex) {
        return errors.response(HttpStatus.NOT_FOUND, ex.getMessage(), "USER_NOT_FOUND", null);
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> clientNotFound(ClientNotFoundException ex) {
        return errors.response(HttpStatus.NOT_FOUND, ex.getMessage(), "CLIENT_NOT_FOUND", null);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> projectNotFound(ProjectNotFoundException ex) {
        return errors.response(HttpStatus.NOT_FOUND, ex.getMessage(), "PROJECT_NOT_FOUND", null);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> taskNotFound(TaskNotFoundException ex) {
        return errors.response(HttpStatus.NOT_FOUND, ex.getMessage(), "TASK_NOT_FOUND", null);
    }

    @ExceptionHandler(TimeEntryNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> timeEntryNotFound(TimeEntryNotFoundException ex) {
        return errors.response(HttpStatus.NOT_FOUND, ex.getMessage(), "TIME_ENTRY_NOT_FOUND", null);
    }

    @ExceptionHandler(RunningTimerNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> runningTimerNotFound(RunningTimerNotFoundException ex) {
        return errors.response(HttpStatus.NOT_FOUND, ex.getMessage(), "RUNNING_TIMER_NOT_FOUND", null);
    }

    @ExceptionHandler(TimerAlreadyRunningException.class)
    public ResponseEntity<ApiResult<Void>> timerAlreadyRunning(TimerAlreadyRunningException ex) {
        return errors.response(HttpStatus.CONFLICT, ex.getMessage(), "TIMER_ALREADY_RUNNING", null);
    }

    @ExceptionHandler(TimeEntryLockedException.class)
    public ResponseEntity<ApiResult<Void>> timeEntryLocked(TimeEntryLockedException ex) {
        return errors.response(HttpStatus.CONFLICT, ex.getMessage(), "TIME_ENTRY_LOCKED", null);
    }

    @ExceptionHandler({BadCredentialsException.class, InvalidCredentialsException.class})
    public ResponseEntity<ApiResult<Void>> invalidCredentials(Exception ex) {
        String message = ex instanceof BadCredentialsException ? "อีเมลหรือรหัสผ่านไม่ถูกต้อง" : ex.getMessage();
        return errors.response(HttpStatus.UNAUTHORIZED, message, "INVALID_CREDENTIALS", null);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResult<Void>> invalidRefreshToken(InvalidRefreshTokenException ex) {
        return errors.response(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_REFRESH_TOKEN", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(field ->
                fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return errors.validation(fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResult<Void>> invalidRequest(Exception ex) {
        return errors.response(HttpStatus.BAD_REQUEST, "ข้อมูลที่ส่งมาไม่ถูกต้อง", "INVALID_REQUEST", null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResult<Void>> responseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return errors.response(status, ex.getReason() == null ? status.getReasonPhrase() : ex.getReason(),
                status.name(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> invalidArgument(IllegalArgumentException ex) {
        return errors.response(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_ARGUMENT", null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResult<Void>> invalidState(IllegalStateException ex) {
        return errors.response(HttpStatus.CONFLICT, ex.getMessage(), "INVALID_STATE", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResult<Void>> noResource(NoResourceFoundException ex) {
        return errors.response(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลที่ร้องขอ", "NOT_FOUND", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> unexpected(Exception ex) {
        return errors.response(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในระบบ",
                "INTERNAL_SERVER_ERROR", null);
    }
}
