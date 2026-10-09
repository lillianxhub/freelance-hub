package th.ac.kku.freelance_hub.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class TimeEntryLockedException extends ApiException {
    public TimeEntryLockedException(UUID id) {
        super(HttpStatus.CONFLICT, "TIME_ENTRY_LOCKED", "รายการเวลาถูกล็อกแล้ว ไม่สามารถแก้ไขได้", Map.of("id", id));
    }
}
