package th.ac.kku.freelance_hub.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class TimeEntryNotFoundException extends ApiException {
    public TimeEntryNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "TIME_ENTRY_NOT_FOUND", "ไม่พบรายการเวลา", Map.of("id", id));
    }
}
