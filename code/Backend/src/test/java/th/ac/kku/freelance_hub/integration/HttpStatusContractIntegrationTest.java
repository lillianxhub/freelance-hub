package th.ac.kku.freelance_hub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import th.ac.kku.freelance_hub.common.response.RequestTraceFilter;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HttpStatusContractIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestTraceFilter requestTraceFilter;

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
    void protectedUserDashboardAndReportRoutesReturnTraceable401() throws Exception {
        for (String route : new String[] {
                "/api/users/me", "/api/dashboard", "/api/reports/summary"
        }) {
            var response = mockMvc.perform(get(route))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
                    .andExpect(jsonPath("$.error.status").value(401))
                    .andReturn().getResponse();
            JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.path("error").path("traceId").asText())
                    .isEqualTo(response.getHeader(RequestTraceFilter.HEADER));
        }
    }

    @Test
    void reportSelectionsForMissingClientOrProjectReturn404() throws Exception {
        String token = registerAndGetToken();
        UUID missingId = UUID.randomUUID();

        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
                get("/api/reports/summary").param("clientId", missingId.toString()),
                get("/api/reports/projects").param("projectId", missingId.toString())
        }) {
            var response = mockMvc.perform(request.header("Authorization", bearer(token)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.error.status").value(404))
                    .andReturn().getResponse();
            JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.path("error").path("traceId").asText())
                    .isEqualTo(response.getHeader(RequestTraceFilter.HEADER));
        }
    }

    @Test
    void publicLogoutWithoutRefreshCookieReturns204WithEmptyBody() throws Exception {
        var response = mockMvc.perform(post("/api/auth/logout")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(response.getContentAsByteArray()).isEmpty();
    }

    private String registerAndGetToken() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Registration(email, PASSWORD, "Status Test"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Registration(String email, String password, String displayName) { }
}
