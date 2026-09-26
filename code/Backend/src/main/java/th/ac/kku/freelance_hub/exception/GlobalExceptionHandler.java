package th.ac.kku.freelance_hub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(EmailAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error("Conflict")
                                .message(ex.getMessage())
                                .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(UserNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message(ex.getMessage())
                                .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(ClientNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleClientNotFound(ClientNotFoundException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message(ex.getMessage())
                                .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message("Invalid email or password")
                                .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message("Invalid email or password")
                                .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
                        MethodArgumentNotValidException ex) {
                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach(error -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                ValidationErrorResponse response = ValidationErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Validation Failed")
                                .message("Invalid input")
                                .errors(errors)
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        //
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleInvalidArgument(
                        IllegalArgumentException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleInvalidState(
                        IllegalStateException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error("Conflict")
                                .message(ex.getMessage())
                                .build();

                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error("Internal Server Error")
                                .message(ex.getMessage())
                                .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        // Project and Tash GlobalHandler
        @ExceptionHandler(ProjectNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleProjectNotFound(
                        ProjectNotFoundException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message(ex.getMessage())
                                .build();

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(error);
        }

        @ExceptionHandler(TaskNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleTaskNotFound(
                        TaskNotFoundException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message(ex.getMessage())
                                .build();

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(error);
        }

        @ExceptionHandler(TimeEntryNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleTimeEntryNotFound(
                        TimeEntryNotFoundException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message(ex.getMessage())
                                .build();

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(error);
        }

        @ExceptionHandler(RunningTimerNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleRunningTimerNotFound(
                        RunningTimerNotFoundException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message(ex.getMessage())
                                .build();

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(error);
        }

        @ExceptionHandler(TimerAlreadyRunningException.class)
        public ResponseEntity<ErrorResponse> handleTimerAlreadyRunning(
                        TimerAlreadyRunningException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error("Conflict")
                                .message(ex.getMessage())
                                .build();

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(error);
        }

        @ExceptionHandler(TimeEntryLockedException.class)
        public ResponseEntity<ErrorResponse> handleTimeEntryLocked(
                        TimeEntryLockedException ex) {
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error("Conflict")
                                .message(ex.getMessage())
                                .build();

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(error);
        }
}
