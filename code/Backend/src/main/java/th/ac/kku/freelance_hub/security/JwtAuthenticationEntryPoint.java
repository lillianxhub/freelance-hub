package th.ac.kku.freelance_hub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import th.ac.kku.freelance_hub.exception.handling.ErrorContext;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityErrorResponseWriter errors;
    public JwtAuthenticationEntryPoint(SecurityErrorResponseWriter errors) { this.errors = errors; }
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        errors.write(exception, new ErrorContext(ErrorContext.Source.AUTHENTICATION, request.getRequestURI()), response);
    }
}
