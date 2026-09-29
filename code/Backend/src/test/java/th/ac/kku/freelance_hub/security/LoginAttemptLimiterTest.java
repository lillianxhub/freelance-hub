package th.ac.kku.freelance_hub.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptLimiterTest {
    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

    @Test
    void blocksAfterTenAccountFailuresAndResetsAfterFifteenMinutes() {
        for (int i = 0; i < 10; i++) limiter.recordFailure("user@example.com", "192.0.2.1");
        assertThat(limiter.retryAfter("user@example.com", "192.0.2.2")).isEqualTo(900);
        clock.advanceSeconds(900);
        assertThat(limiter.retryAfter("user@example.com", "192.0.2.2")).isZero();
    }

    @Test
    void blocksAddressAfterThreeHundredFailuresAndAllowsAnotherAddress() {
        for (int i = 0; i < 300; i++) limiter.recordFailure("user" + i + "@example.com", "192.0.2.1");
        assertThat(limiter.retryAfter("fresh@example.com", "192.0.2.1")).isEqualTo(60);
        assertThat(limiter.retryAfter("fresh@example.com", "192.0.2.2")).isZero();
        clock.advanceSeconds(60);
        assertThat(limiter.retryAfter("fresh@example.com", "192.0.2.1")).isZero();
    }

    @Test
    void successfulLoginClearsOnlyItsAccountFailures() {
        for (int i = 0; i < 10; i++) limiter.recordFailure("user@example.com", "192.0.2.1");
        limiter.recordSuccess("user@example.com");
        assertThat(limiter.retryAfter("user@example.com", "192.0.2.2")).isZero();
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-29T00:00:00Z");

        void advanceSeconds(long seconds) { now = now.plusSeconds(seconds); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
