package th.ac.kku.freelance_hub.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class LoginRateLimitedException extends ApiException {
    private final long retryAfterSeconds;
    public LoginRateLimitedException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_RATE_LIMITED",
                "ลองเข้าสู่ระบบบ่อยเกินไป กรุณารอสักครู่", Map.of("retryAfterSeconds", retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
