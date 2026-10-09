package th.ac.kku.freelance_hub.exception;

import org.springframework.http.HttpStatus;

public class RunningTimerNotFoundException extends ApiException {
    public RunningTimerNotFoundException() {
        super(HttpStatus.NOT_FOUND, "RUNNING_TIMER_NOT_FOUND", "ไม่พบตัวจับเวลาที่กำลังทำงาน", null);
    }
}
