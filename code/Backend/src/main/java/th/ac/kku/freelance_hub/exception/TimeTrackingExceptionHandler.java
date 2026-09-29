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
import th.ac.kku.freelance_hub.controller.TimeEntryController;
import th.ac.kku.freelance_hub.controller.TimerController;

/** Applies the shared response envelope to Time Entry and Timer errors. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {TimeEntryController.class, TimerController.class})
public class TimeTrackingExceptionHandler {

    @ExceptionHandler(TimeEntryNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> timeEntryNotFound(TimeEntryNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "ไม่พบรายการเวลา", "TIME_ENTRY_NOT_FOUND", null);
    }

    @ExceptionHandler(RunningTimerNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> runningTimerNotFound(RunningTimerNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "ไม่พบตัวจับเวลาที่กำลังทำงาน", "RUNNING_TIMER_NOT_FOUND", null);
    }

    @ExceptionHandler(TimerAlreadyRunningException.class)
    public ResponseEntity<ApiResult<Void>> timerAlreadyRunning(TimerAlreadyRunningException ex) {
        return error(HttpStatus.CONFLICT, "มีตัวจับเวลาที่กำลังทำงานอยู่แล้ว", "TIMER_ALREADY_RUNNING", null);
    }

    @ExceptionHandler(TimeEntryLockedException.class)
    public ResponseEntity<ApiResult<Void>> timeEntryLocked(TimeEntryLockedException ex) {
        return error(HttpStatus.CONFLICT, "รายการเวลาถูกล็อก", "TIME_ENTRY_LOCKED", null);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> projectNotFound(ProjectNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "ไม่พบโปรเจกต์", "PROJECT_NOT_FOUND", null);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> taskNotFound(TaskNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "ไม่พบงาน", "TASK_NOT_FOUND", null);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> userNotFound(UserNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้", "USER_NOT_FOUND", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "ข้อมูลที่ส่งมาไม่ถูกต้อง", "VALIDATION_ERROR", fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResult<Void>> malformedRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "ข้อมูลที่ส่งมาไม่ถูกต้อง", "INVALID_REQUEST", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> invalidArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "ข้อมูลที่ส่งมาไม่ถูกต้อง", "INVALID_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResult<Void>> invalidState(IllegalStateException ex) {
        return error(HttpStatus.CONFLICT, "ไม่สามารถดำเนินการกับสถานะปัจจุบันได้", "INVALID_STATE", ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResult<Void>> responseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return error(status, "ไม่สามารถดำเนินการตามคำขอได้", status.name(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> unexpected(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในระบบ", "INTERNAL_SERVER_ERROR", null);
    }

    private ResponseEntity<ApiResult<Void>> error(HttpStatus status, String message, String code, Object details) {
        return ResponseEntity.status(status).body(ApiResult.error(message, code, details));
    }
}
