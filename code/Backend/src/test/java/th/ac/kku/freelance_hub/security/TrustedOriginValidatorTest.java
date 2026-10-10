package th.ac.kku.freelance_hub.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import th.ac.kku.freelance_hub.exception.OriginNotAllowedException;

class TrustedOriginValidatorTest {
    private TrustedOriginValidator validator() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://app.example.com", "http://localhost:5173"));
        return new TrustedOriginValidator(request -> config);
    }

    @Test
    void acceptsAllowedOriginAndRefererIncludingPort() {
        for (String origin : List.of("https://app.example.com", "http://localhost:5173")) {
            var request = new MockHttpServletRequest();
            request.addHeader("Origin", origin);
            assertThatCode(() -> validator().verify(request)).doesNotThrowAnyException();
            var refererRequest = new MockHttpServletRequest();
            refererRequest.addHeader("Referer", origin + "/workspace?tab=1");
            assertThatCode(() -> validator().verify(refererRequest)).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"null", "/relative", "not a uri", "https://evil.example.com/path",
            "https://user:password@app.example.com/path", "https://app.example.com.evil/path"})
    void rejectsMissingMalformedAndUntrustedReferer(String referer) {
        var request = new MockHttpServletRequest();
        if (referer != null) request.addHeader("Referer", referer);
        assertThatThrownBy(() -> validator().verify(request)).isInstanceOf(OriginNotAllowedException.class);
    }

    @Test
    void untrustedOriginCannotBeOverriddenByTrustedReferer() {
        var request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://evil.example.com");
        request.addHeader("Referer", "https://app.example.com/path");
        assertThatThrownBy(() -> validator().verify(request)).isInstanceOf(OriginNotAllowedException.class);
        assertThatThrownBy(() -> new TrustedOriginValidator(r -> null).verify(request))
                .isInstanceOf(OriginNotAllowedException.class);
    }
}
