package th.ac.kku.freelance_hub.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Single-instance, bounded login throttling. Never use client-supplied forwarding headers as an IP key. */
@Component
public class LoginAttemptLimiter {
    private static final Duration ACCOUNT_WINDOW = Duration.ofMinutes(15);
    private static final Duration ADDRESS_WINDOW = Duration.ofMinutes(1);
    private static final int ACCOUNT_LIMIT = 10;
    private static final int ADDRESS_LIMIT = 300;

    private final Cache<String, Window> attempts = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(ACCOUNT_WINDOW)
            .build();
    private final Clock clock;

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Seconds until the next attempt is allowed; zero means allowed. */
    public long retryAfter(String email, String remoteAddress) {
        Instant now = clock.instant();
        return Math.max(remaining("account:" + email, ACCOUNT_WINDOW, ACCOUNT_LIMIT, now),
                remaining("address:" + remoteAddress, ADDRESS_WINDOW, ADDRESS_LIMIT, now));
    }

    public void recordFailure(String email, String remoteAddress) {
        Instant now = clock.instant();
        increment("account:" + email, ACCOUNT_WINDOW, now);
        increment("address:" + remoteAddress, ADDRESS_WINDOW, now);
    }

    public void recordSuccess(String email) {
        attempts.invalidate("account:" + email);
    }

    private long remaining(String key, Duration duration, int limit, Instant now) {
        Window window = attempts.getIfPresent(key);
        if (window == null || !now.isBefore(window.startedAt().plus(duration)) || window.count() < limit) return 0;
        long millis = Duration.between(now, window.startedAt().plus(duration)).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }

    private void increment(String key, Duration duration, Instant now) {
        attempts.asMap().compute(key, (ignored, old) ->
                old == null || !now.isBefore(old.startedAt().plus(duration))
                        ? new Window(now, 1) : new Window(old.startedAt(), old.count() + 1));
    }

    private record Window(Instant startedAt, int count) { }
}
