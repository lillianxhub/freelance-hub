package th.ac.kku.freelance_hub.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.User;
import th.ac.kku.freelance_hub.common.response.ApiErrorFactory;

class JwtAuthenticationFilterTest {

    @Test
    void downstreamFailureForDisabledUserIsPropagatedWithoutRetry() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                tokenProvider, userDetailsService, th.ac.kku.freelance_hub.support.ErrorHandlingTestSupport.security());
        String email = "disabled@example.com";
        String jwt = "valid-token";
        when(tokenProvider.validateToken(jwt)).thenReturn(true);
        when(tokenProvider.getJtiFromToken(jwt)).thenReturn("token-id");
        when(tokenProvider.getEmailFromToken(jwt)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(User.withUsername(email)
                .password("ignored")
                .disabled(false)
                .authorities("ROLE_USER")
                .build());

        FilterChain downstream = mock(FilterChain.class);
        doThrow(new ServletException("downstream failure"))
                .when(downstream).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, downstream))
                .isInstanceOf(ServletException.class)
                .hasMessage("downstream failure");

        verify(downstream, times(1)).doFilter(request, response);
    }
}
