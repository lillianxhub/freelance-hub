package th.ac.kku.freelance_hub.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ProjectNotFoundException extends ApiException {
    public ProjectNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "ไม่พบโปรเจกต์", Map.of("id", id));
    }
}
