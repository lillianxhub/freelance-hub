package th.ac.kku.freelance_hub.exception;

public class LoginRateLimitedException extends RuntimeException {
    private final long retryAfterSeconds;

    public LoginRateLimitedException(long retryAfterSeconds) {
        super("ลองเข้าสู่ระบบบ่อยเกินไป กรุณารอสักครู่");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
