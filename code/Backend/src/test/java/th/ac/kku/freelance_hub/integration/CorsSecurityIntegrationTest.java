package th.ac.kku.freelance_hub.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties =
        "app.cors.allowed-origins=http://localhost:5173,https://second-origin.test")
class CorsSecurityIntegrationTest {

    private static final String LOCAL_ORIGIN = "http://localhost:5173";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void allowedPreflightPassesThroughSecurityAndReturnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/clients")
                        .header(HttpHeaders.ORIGIN, LOCAL_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        LOCAL_ORIGIN
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        containsString("POST")
                ))
                .andExpect(header().doesNotExist(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS
                ));
    }

    @Test
    void commaSeparatedSecondOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/clients")
                        .header(HttpHeaders.ORIGIN, "https://second-origin.test")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://second-origin.test"
                ));
    }

    @Test
    void allowedOriginIsIncludedOnProtectedResponse() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.ORIGIN, LOCAL_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        LOCAL_ORIGIN
                ));
    }

    @Test
    void actuatorHealthIsPublicForDeploymentProbes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void disallowedOriginDoesNotReceiveAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/api/clients")
                        .header(HttpHeaders.ORIGIN, "https://not-allowed.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
