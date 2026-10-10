package th.ac.kku.freelance_hub.security;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshTokenCookieTest {
    @Test
    void setsRestrictedCookieAndClearsUsingSameAttributes() {
        var cookie = new RefreshTokenCookie();
        ReflectionTestUtils.setField(cookie, "secure", true);
        String header = cookie.set("opaque-token", Instant.now().plusSeconds(3600));
        assertThat(header).contains("fh_refresh=opaque-token", "Path=/api/auth", "HttpOnly", "Secure", "SameSite=Lax", "Max-Age=");
        assertThat(cookie.clear()).contains("fh_refresh=", "Path=/api/auth", "HttpOnly", "Secure", "SameSite=Lax", "Max-Age=0");
        ReflectionTestUtils.setField(cookie, "secure", false);
        assertThat(cookie.clear()).doesNotContain("Secure");
    }
}
