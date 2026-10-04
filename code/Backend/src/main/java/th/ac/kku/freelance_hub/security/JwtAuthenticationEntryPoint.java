package th.ac.kku.freelance_hub.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.common.response.ApiErrorFactory;
import org.springframework.http.HttpStatus;

/**
 * Entry point for handling authentication errors
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        private final ApiErrorFactory errorFactory;

        public JwtAuthenticationEntryPoint(ApiErrorFactory errorFactory) {
                this.errorFactory = errorFactory;
        }

        @Override
        public void commence(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                objectMapper.writeValue(response.getWriter(),
                                errorFactory.body(HttpStatus.UNAUTHORIZED, "Authentication is required",
                                                "AUTHENTICATION_REQUIRED", null));
        }
}
