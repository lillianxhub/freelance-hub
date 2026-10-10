package th.ac.kku.freelance_hub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import th.ac.kku.freelance_hub.repository.RefreshTokenRepository;
import th.ac.kku.freelance_hub.security.JwtTokenProvider;
import th.ac.kku.freelance_hub.common.response.RequestTraceFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import th.ac.kku.freelance_hub.dto.request.auth.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest;
import th.ac.kku.freelance_hub.dto.response.auth.AuthResponse;
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController Tests")
class AuthControllerTest {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private RefreshTokenRepository refreshTokenRepository;

        @Autowired
        private RequestTraceFilter requestTraceFilter;

        @Autowired
        private JwtTokenProvider tokenProvider;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private RegisterRequest registerRequest;
        private LoginRequest loginRequest;

        @Test
        void rejectsPasswordsOverBcryptByteLimitBeforeEncoding() throws Exception {
                for (String password : new String[] {"a".repeat(73), "ก".repeat(25)}) {
                        registerRequest.setPassword(password);
                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(registerRequest)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
                }
                registerRequest.setPassword("password123");
                String token = registerAndGetToken();
                mockMvc.perform(patch("/api/users/me/password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(java.util.Map.of(
                                        "oldPassword", "password123", "newPassword", "ก".repeat(25)))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("OpenAPI documents Auth success responses with the common envelope")
        void shouldDocumentAuthResponseEnvelope() throws Exception {
                String document = mockMvc.perform(get("/v3/api-docs"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                JsonNode root = objectMapper.readTree(document);

                for (String path : new String[] { "/api/auth/login", "/api/auth/refresh" }) {
                        String statusCode = "200";
                        JsonNode schema = root.path("paths").path(path).path("post")
                                        .path("responses").path(statusCode).path("content")
                                        .path("application/json").path("schema");
                        String reference = schema.path("$ref").asText();
                        assertThat(reference).as(path + " response: " + root.path("paths").path(path).path("post").path("responses")).startsWith("#/components/schemas/");
                        JsonNode envelope = root.path("components").path("schemas")
                                        .path(reference.substring(reference.lastIndexOf('/') + 1));
                        assertThat(envelope.path("properties").has("success")).as(path).isTrue();
                        assertThat(envelope.path("properties").has("message")).as(path).isTrue();
                        assertThat(envelope.path("properties").has("data")).as(path).isTrue();
                        assertThat(envelope.path("properties").path("data").path("$ref").asText())
                                        .as(path + " data schema")
                                        .isEqualTo("#/components/schemas/AuthResponse");
                }
        }

        @Test
        void openApiDocumentsFailureOnlyForBadRequestsAndUserForRegistration() throws Exception {
                JsonNode root = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString());
                JsonNode badRequest = root.path("paths").path("/api/auth/register").path("post")
                                .path("responses").path("400").path("content")
                                .path("application/json").path("schema");
                assertThat(badRequest.path("allOf").get(1).path("properties").path("success")
                                .path("enum").get(0).asBoolean()).isFalse();
                assertThat(root.path("paths").path("/api/auth/register").path("post")
                                .path("responses").path("201").path("content")
                                .path("application/json").path("schema").path("$ref").asText())
                                .startsWith("#/components/schemas/ApiResult");
                assertThat(badRequest.toString()).doesNotContain("HTTP_400");
        }

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                .addFilters(requestTraceFilter)
                                .apply(springSecurity())
                                .build();

                registerRequest = RegisterRequest.builder()
                                .email("test@example.com")
                                .password("password123")
                                .displayName("Test User")
                                .firstName("Test")
                                .lastName("User")
                                .phone("0812345678")
                                .build();

                loginRequest = LoginRequest.builder()
                                .email("test@example.com")
                                .password("password123")
                                .build();
        }

        @Test
        @DisplayName("POST /api/auth/register - Should register user successfully")
        void shouldRegisterUserSuccessfully() throws Exception {
                // When & Then
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("สมัครสมาชิกสำเร็จ"))
                                .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
                                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()))
                                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                                .andExpect(jsonPath("$.data.user.displayName").value("Test User"))
                                .andExpect(jsonPath("$.data.token").isNotEmpty())
                                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));
        }

        @Test
        @DisplayName("POST /api/auth/register - Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
                // Given
                registerRequest.setEmail("invalid-email");

                // When & Then
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("ข้อมูลที่ส่งมาไม่ถูกต้อง"))
                                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                                .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
                                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.error.fieldErrors.email").value("รูปแบบอีเมลไม่ถูกต้อง"));
        }

        @Test
        @DisplayName("POST /api/auth/register - Should return 400 when password is too short")
        void shouldReturn400WhenPasswordIsTooShort() throws Exception {
                // Given
                registerRequest.setPassword("short");

                // When & Then
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/auth/register - Should return 400 when required fields are missing")
        void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
                // Given
                RegisterRequest invalidRequest = RegisterRequest.builder().build();

                // When & Then
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/auth/register - Should return 409 when email already exists")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
                // Given
                // Register first user
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)));

                // When & Then - Try to register with same email
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"))
                                .andExpect(jsonPath("$.message").value("อีเมลนี้ถูกใช้งานแล้ว"));
        }

        @Test
        @DisplayName("POST /api/auth/login - Should login user successfully")
        void shouldLoginUserSuccessfully() throws Exception {
                // Given - Register user first
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)));

                // When & Then - Login with registered credentials
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("เข้าสู่ระบบสำเร็จ"))
                                .andExpect(jsonPath("$.data.token").exists())
                                .andExpect(jsonPath("$.data.expiresIn").exists())
                                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
        }

        @Test
        @DisplayName("POST /api/auth/login - Should return 400 when email is missing")
        void shouldReturn400WhenEmailIsMissing() throws Exception {
                // Given
                loginRequest.setEmail(null);

                // When & Then
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("ข้อมูลที่ส่งมาไม่ถูกต้อง"))
                                .andExpect(jsonPath("$.error.fieldErrors.email").value("กรุณาระบุอีเมล"));
        }

        @Test
        @DisplayName("POST /api/auth/login - Should return 400 when password is missing")
        void shouldReturn400WhenPasswordIsMissing() throws Exception {
                // Given
                loginRequest.setPassword(null);

                // When & Then
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/auth/login - Should return 401 when credentials are invalid")
        void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
                // Given
                LoginRequest invalidLogin = LoginRequest.builder()
                                .email("test@example.com")
                                .password("wrongpassword")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidLogin)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                                .andExpect(jsonPath("$.message").value("อีเมลหรือรหัสผ่านไม่ถูกต้อง"));
        }

        @Test
        @DisplayName("POST /api/auth/logout revokes the refresh family and returns 204")
        void shouldLogoutAndRevokeRefreshFamily() throws Exception {
                String accessToken = registerAndGetToken();
                Cookie cookie = loginAndGetCookie();
                mockMvc.perform(post("/api/auth/logout").header("Origin", "http://localhost:5173").cookie(cookie))
                                .andExpect(status().isNoContent())
                                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
                mockMvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173").cookie(cookie))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("เซสชันหมดอายุหรือไม่ถูกต้อง กรุณาเข้าสู่ระบบใหม่"));
                // Without an access-token denylist, the existing JWT lives until its 15-minute expiry.
                mockMvc.perform(get("/api/users/me").header("Authorization", bearer(accessToken)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/auth/logout is idempotent")
        void shouldAllowRepeatedLogout() throws Exception {
                mockMvc.perform(post("/api/auth/logout").header("Origin", "http://localhost:5173")).andExpect(status().isNoContent());
                mockMvc.perform(post("/api/auth/logout").header("Origin", "http://localhost:5173")).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Login after logout should issue a new usable token")
        void shouldIssueUsableTokenAfterLoginFollowingLogout() throws Exception {
                String oldToken = registerAndGetToken();
                Cookie cookie = loginAndGetCookie();
                mockMvc.perform(post("/api/auth/logout").header("Origin", "http://localhost:5173").cookie(cookie))
                                .andExpect(status().isNoContent());

                String loginBody = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                String newToken = objectMapper.readTree(loginBody).path("data").path("token").asText();

                assertThat(newToken).isNotEqualTo(oldToken);
                mockMvc.perform(get("/api/users/me")
                                .header("Authorization", bearer(newToken)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PATCH /api/users/me/password - Should change password with old and new values")
        void shouldChangePasswordUsingOldAndNewPassword() throws Exception {
                String token = registerAndGetToken();
                Cookie refreshCookie = loginAndGetCookie();

                mockMvc.perform(patch("/api/users/me/password")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"oldPassword\":\"password123\",\"newPassword\":\"newPassword123\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"test@example.com\",\"password\":\"newPassword123\"}"))
                                .andExpect(status().isOk());
                mockMvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173").cookie(refreshCookie))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Wrong old password has its own Thai error message")
        void shouldExplainWrongOldPassword() throws Exception {
                String token = registerAndGetToken();
                mockMvc.perform(patch("/api/users/me/password")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"oldPassword\":\"wrong-password\",\"newPassword\":\"newPassword123\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("รหัสผ่านไม่ถูกต้อง"));
        }

        @Test
        @DisplayName("PATCH /api/users/me - Should update profile and normalized address")
        void shouldUpdateProfileAddressAsFlatFields() throws Exception {
                String token = registerAndGetToken();

                mockMvc.perform(patch("/api/users/me")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"address\":\"99 ถนนมิตรภาพ\",\"subdistrict\":\"ในเมือง\","
                                                + "\"district\":\"เมืองขอนแก่น\",\"province\":\"ขอนแก่น\","
                                                + "\"postalCode\":\"40000\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.address").value("99 ถนนมิตรภาพ"))
                                .andExpect(jsonPath("$.data.subdistrict").value("ในเมือง"))
                                .andExpect(jsonPath("$.data.district").value("เมืองขอนแก่น"))
                                .andExpect(jsonPath("$.data.province").value("ขอนแก่น"))
                                .andExpect(jsonPath("$.data.postalCode").value("40000"));
        }

        @Test
        @DisplayName("POST /api/auth/refresh rejects missing and malformed refresh cookies")
        void shouldRejectMissingAndMalformedRefreshTokens() throws Exception {
                mockMvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("เซสชันหมดอายุหรือไม่ถูกต้อง กรุณาเข้าสู่ระบบใหม่"));
                mockMvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173")
                                .cookie(new Cookie("fh_refresh", "malformed-token")))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Refresh and logout reject an unapproved browser origin")
        void shouldRejectUnapprovedOrigin() throws Exception {
                mockMvc.perform(post("/api/auth/refresh").header("Origin", "https://attacker.example"))
                                .andExpect(status().isForbidden());
                mockMvc.perform(post("/api/auth/logout").header("Origin", "https://attacker.example"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Too many bad logins return 429 with Retry-After")
        void shouldThrottleBadLogins() throws Exception {
                String email = "throttle-" + java.util.UUID.randomUUID() + "@example.com";
                registerRequest.setEmail(email);
                registerAndGetToken();
                String badLogin = "{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}";
                for (int i = 0; i < 10; i++) {
                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON).content(badLogin))
                                        .andExpect(status().isUnauthorized());
                }
                var throttled = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON).content(badLogin))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(header().exists("Retry-After"))
                                .andExpect(jsonPath("$.error.code").value("LOGIN_RATE_LIMITED"))
                                .andExpect(jsonPath("$.error.status").value(429))
                                .andReturn().getResponse();
                JsonNode error = objectMapper.readTree(throttled.getContentAsString()).path("error");
                assertThat(error.path("traceId").asText()).isEqualTo(throttled.getHeader("X-Request-ID"));
                java.time.Instant.parse(error.path("timestamp").asText());
        }

        @Test
        @DisplayName("Refresh and logout reject requests without Origin or Referer")
        void shouldRejectMissingOriginAndReferer() throws Exception {
                mockMvc.perform(post("/api/auth/refresh"))
                                .andExpect(status().isForbidden());
                mockMvc.perform(post("/api/auth/logout"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Refresh and logout accept a trusted Referer when Origin is absent")
        void shouldAcceptTrustedReferer() throws Exception {
                mockMvc.perform(post("/api/auth/logout")
                                .header("Referer", "http://localhost:5173/dashboard"))
                                .andExpect(status().isNoContent());
                mockMvc.perform(post("/api/auth/refresh")
                                .header("Referer", "http://localhost:5173/dashboard"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Untrusted Origin takes precedence over trusted Referer")
        void shouldNotTrustRefererIfOriginIsUntrusted() throws Exception {
                mockMvc.perform(post("/api/auth/logout")
                                .header("Origin", "https://attacker.example")
                                .header("Referer", "http://localhost:5173/dashboard"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/auth/refresh rotates cookie and detects replay")
        void shouldRotateAndRejectReplayedRefreshToken() throws Exception {
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated());
                Cookie first = loginAndGetCookie();
                var rotated = mockMvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173").cookie(first))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Token refreshed"))
                                .andExpect(jsonPath("$.data.token").exists())
                                .andReturn();
                Cookie second = cookieFrom(rotated.getResponse().getHeader("Set-Cookie"));
                assertThat(second.getValue()).isNotEqualTo(first.getValue());
                var stored = refreshTokenRepository.findAll().stream()
                                .filter(token -> token.getUser().getEmail().equals(registerRequest.getEmail()))
                                .toList();
                assertThat(stored).hasSize(3);
                assertThat(stored).filteredOn(token -> token.getUsedAt() != null).hasSize(1);
                assertThat(stored).filteredOn(token -> token.getRevokedAt() == null).hasSize(3);
                mockMvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173").cookie(first))
                                .andExpect(status().isUnauthorized());
                mockMvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173").cookie(second))
                                .andExpect(status().isUnauthorized());
                assertThat(refreshTokenRepository.findAll().stream()
                                .filter(token -> token.getUser().getEmail().equals(registerRequest.getEmail()))
                                .toList()).allSatisfy(token ->
                                assertThat(token.getTokenHash()).isNotEqualTo(first.getValue()));
        }

        private Cookie loginAndGetCookie() throws Exception {
                String header = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
                                .andReturn().getResponse().getHeader("Set-Cookie");
                return cookieFrom(header);
        }

        private static Cookie cookieFrom(String header) {
                return new Cookie("fh_refresh", header.split(";", 2)[0].split("=", 2)[1]);
        }

        private String registerAndGetToken() throws Exception {
                loginRequest.setEmail(registerRequest.getEmail());
                String body = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                return objectMapper.readTree(body).path("data").path("token").asText();
        }

        private static String bearer(String token) {
                return "Bearer " + token;
        }
}
