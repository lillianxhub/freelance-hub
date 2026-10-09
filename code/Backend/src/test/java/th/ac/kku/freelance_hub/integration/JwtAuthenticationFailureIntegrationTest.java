package th.ac.kku.freelance_hub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import th.ac.kku.freelance_hub.common.response.RequestTraceFilter;
import th.ac.kku.freelance_hub.security.CustomUserDetailsService;
import th.ac.kku.freelance_hub.security.JwtTokenProvider;

@SpringBootTest
@ActiveProfiles("test")
class JwtAuthenticationFailureIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestTraceFilter requestTraceFilter;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestTraceFilter)
                .apply(springSecurity())
                .build();
    }

    @Test
    void userStoreFailureWhileReadingValidJwtReturns500EnvelopeOnce() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String token = tokenProvider.generateToken(email);
        when(userDetailsService.loadUserByUsername(email))
                .thenThrow(new DataAccessResourceFailureException("internal database details"));

        var response = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("เกิดข้อผิดพลาดภายในระบบ"))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error.status").value(500))
                .andReturn().getResponse();

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.path("error").path("traceId").asText())
                .isEqualTo(response.getHeader(RequestTraceFilter.HEADER));
        assertThat(response.getContentAsString()).doesNotContain("internal database details");
        verify(userDetailsService, times(1)).loadUserByUsername(email);
    }

    @Test
    void validTokenForDeletedUserRemains401() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        when(userDetailsService.loadUserByUsername(email))
                .thenThrow(new UsernameNotFoundException("user no longer exists"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenProvider.generateToken(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.status").value(401));
        verify(userDetailsService, times(1)).loadUserByUsername(email);
    }
}
