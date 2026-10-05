package th.ac.kku.freelance_hub.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.server.ResponseStatusException;

/** Checks cookie-backed actions against the same origin allowlist used by CORS. */
@Component
public class TrustedOriginValidator {
    private final CorsConfigurationSource corsConfigurationSource;

    public TrustedOriginValidator(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    public void verify(HttpServletRequest request) {
        String source = request.getHeader("Origin");
        if (source == null) source = originFromReferer(request.getHeader("Referer"));
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);
        if (source == null || config == null || config.checkOrigin(source) == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origin not allowed");
        }
    }

    private String originFromReferer(String referer) {
        if (referer == null) return null;
        try {
            URI uri = URI.create(referer);
            if (uri.getScheme() == null || uri.getHost() == null || uri.getUserInfo() != null) return null;
            return uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
