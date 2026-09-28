package th.ac.kku.freelance_hub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import th.ac.kku.freelance_hub.dto.request.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.RegisterRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        private JwtTokenProvider tokenProvider;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private RegisterRequest registerRequest;
        private LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
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
                                .andExpect(jsonPath("$.data.token").exists())
                                .andExpect(jsonPath("$.data.expiresIn").exists())
                                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                                .andExpect(jsonPath("$.data.user.displayName").value("Test User"));
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
                                .andExpect(status().isBadRequest());
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
                                .andExpect(status().isConflict());
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
                                .andExpect(status().isBadRequest());
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
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/logout revokes the refresh family and returns 204")
        void shouldLogoutAndRevokeRefreshFamily() throws Exception {
                String accessToken = registerAndGetToken();
                Cookie cookie = loginAndGetCookie();
                mockMvc.perform(post("/api/auth/logout").cookie(cookie))
                                .andExpect(status().isNoContent())
                                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
                mockMvc.perform(post("/api/auth/refresh").cookie(cookie))
                                .andExpect(status().isUnauthorized());
                // Without an access-token denylist, the existing JWT lives until its 15-minute expiry.
                mockMvc.perform(get("/api/users/me").header("Authorization", bearer(accessToken)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/auth/logout is idempotent")
        void shouldAllowRepeatedLogout() throws Exception {
                mockMvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
                mockMvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Login after logout should issue a new usable token")
        void shouldIssueUsableTokenAfterLoginFollowingLogout() throws Exception {
                String oldToken = registerAndGetToken();
                Cookie cookie = loginAndGetCookie();
                mockMvc.perform(post("/api/auth/logout").cookie(cookie))
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
                mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
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
                mockMvc.perform(post("/api/auth/refresh"))
                                .andExpect(status().isUnauthorized());
                mockMvc.perform(post("/api/auth/refresh")
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
        @DisplayName("POST /api/auth/refresh rotates cookie and detects replay")
        void shouldRotateAndRejectReplayedRefreshToken() throws Exception {
                registerAndGetToken();
                Cookie first = loginAndGetCookie();
                var rotated = mockMvc.perform(post("/api/auth/refresh").cookie(first))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.token").exists())
                                .andReturn();
                Cookie second = cookieFrom(rotated.getResponse().getHeader("Set-Cookie"));
                assertThat(second.getValue()).isNotEqualTo(first.getValue());
                var stored = refreshTokenRepository.findAll();
                assertThat(stored).hasSize(2);
                assertThat(stored.get(0).getFamilyId()).isEqualTo(stored.get(1).getFamilyId());
                assertThat(stored.get(0).getExpiresAt()).isEqualTo(stored.get(1).getExpiresAt());
                mockMvc.perform(post("/api/auth/refresh").cookie(first))
                                .andExpect(status().isUnauthorized());
                mockMvc.perform(post("/api/auth/refresh").cookie(second))
                                .andExpect(status().isUnauthorized());
                assertThat(refreshTokenRepository.findAll()).allSatisfy(token ->
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
