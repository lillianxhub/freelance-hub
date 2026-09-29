package th.ac.kku.freelance_hub.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class RefreshTokenCookie {
    public static final String NAME = "fh_refresh";

    @Value("${app.auth.refresh-cookie-secure:true}")
    private boolean secure;

    public String set(String token, Instant expiresAt) {
        return base(token).maxAge(Duration.between(Instant.now(), expiresAt)).build().toString();
    }

    public String clear() {
        return base("").maxAge(Duration.ZERO).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true).secure(secure).sameSite("Lax").path("/api/auth");
    }
}
