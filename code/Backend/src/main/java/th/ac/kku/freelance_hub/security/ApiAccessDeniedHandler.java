package th.ac.kku.freelance_hub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import th.ac.kku.freelance_hub.exception.handling.ErrorContext;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityErrorResponseWriter errors;
    public ApiAccessDeniedHandler(SecurityErrorResponseWriter errors) { this.errors = errors; }
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        errors.write(exception, new ErrorContext(ErrorContext.Source.AUTHORIZATION, request.getRequestURI()), response);
    }
}
