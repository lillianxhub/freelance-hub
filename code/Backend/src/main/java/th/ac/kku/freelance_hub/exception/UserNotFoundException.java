package th.ac.kku.freelance_hub.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "ไม่พบผู้ใช้", Map.of("id", id));
    }
    public UserNotFoundException(String email) {
        super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "ไม่พบผู้ใช้", null);
    }
}
