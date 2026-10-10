package th.ac.kku.freelance_hub.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class TaskNotFoundException extends ApiException {
    public TaskNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "ไม่พบงาน", Map.of("id", id));
    }
}
