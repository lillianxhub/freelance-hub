package th.ac.kku.freelance_hub.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import th.ac.kku.freelance_hub.common.response.RequestTraceFilter;
import th.ac.kku.freelance_hub.support.ErrorHandlingTestSupport;

class SecurityErrorResponseTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void entryPointAndAccessDeniedUseSharedTraceableEnvelope() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/users/me");
        for (boolean denied : List.of(false, true)) {
            var response = new MockHttpServletResponse();
            new RequestTraceFilter().doFilter(request, response, (req, res) -> {
                if (denied) new ApiAccessDeniedHandler(ErrorHandlingTestSupport.security()).handle(
                        request, response, new AccessDeniedException("internal"));
                else new JwtAuthenticationEntryPoint(ErrorHandlingTestSupport.security()).commence(
                        request, response, new BadCredentialsException("internal"));
            });
            var body = mapper.readTree(response.getContentAsString());
            assertThat(response.getStatus()).isEqualTo(denied ? 403 : 401);
            assertThat(body.path("error").path("code").asText()).isEqualTo(denied ? "ACCESS_DENIED" : "AUTHENTICATION_REQUIRED");
            assertThat(body.path("error").path("status").asInt()).isEqualTo(response.getStatus());
            assertThat(body.path("error").path("traceId").asText()).isEqualTo(response.getHeader(RequestTraceFilter.HEADER));
            assertThat(body.path("data").isNull()).isTrue();
            assertThat(body.path("meta").isNull()).isTrue();
            assertThat(response.getContentAsString()).doesNotContain("internal");
        }
    }

    @Test
    void jwtDependencyFailureIs500AndDoesNotContinueTheRequest() throws Exception {
        var provider = mock(JwtTokenProvider.class);
        var users = mock(CustomUserDetailsService.class);
        when(provider.validateToken("valid")).thenReturn(true);
        when(provider.getJtiFromToken("valid")).thenReturn("jti");
        when(provider.getEmailFromToken("valid")).thenReturn("user@example.com");
        when(users.loadUserByUsername("user@example.com")).thenThrow(new BadCredentialsException("SQL secret"));
        var filter = new JwtAuthenticationFilter(provider, users, ErrorHandlingTestSupport.security());
        var request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer valid");
        var response = new MockHttpServletResponse();
        var downstream = mock(FilterChain.class);
        try {
            new RequestTraceFilter().doFilter(request, response, (req, res) -> filter.doFilter(req, res, downstream));
            var body = mapper.readTree(response.getContentAsString());
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(body.path("error").path("code").asText()).isEqualTo("INTERNAL_SERVER_ERROR");
            assertThat(body.path("error").path("traceId").asText()).isEqualTo(response.getHeader(RequestTraceFilter.HEADER));
            assertThat(response.getContentAsString()).doesNotContain("SQL", "secret");
            verifyNoInteractions(downstream);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
