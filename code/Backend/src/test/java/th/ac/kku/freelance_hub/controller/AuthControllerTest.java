package th.ac.kku.freelance_hub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import th.ac.kku.freelance_hub.repository.RevokedTokenRepository;
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
        private RevokedTokenRepository revokedTokenRepository;

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
                                .timezone("Asia/Bangkok")
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
                                .andExpect(jsonPath("$.token").exists())
                                .andExpect(jsonPath("$.expiresIn").exists())
                                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                                .andExpect(jsonPath("$.user.displayName").value("Test User"));
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
                                .andExpect(jsonPath("$.token").exists())
                                .andExpect(jsonPath("$.expiresIn").exists())
                                .andExpect(jsonPath("$.user.email").value("test@example.com"));
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
        @DisplayName("POST /api/auth/logout - Should revoke token and return 204")
        void shouldLogoutAndPersistOnlyTokenIdentifier() throws Exception {
                String token = registerAndGetToken();
                String jti = tokenProvider.getJtiFromToken(token);

                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", bearer(token)))
                                .andExpect(status().isNoContent())
                                .andExpect(content().string(""));

                var revokedToken = revokedTokenRepository.findByJti(jti).orElseThrow();
                assertThat(revokedToken.getJti()).isNotEqualTo(token);
                assertThat(revokedToken.getUser().getEmail()).isEqualTo(registerRequest.getEmail());
                assertThat(revokedToken.getExpiresAt())
                                .isCloseTo(tokenProvider.getExpirationFromToken(token).toInstant(),
                                                within(1, java.time.temporal.ChronoUnit.SECONDS));

                mockMvc.perform(get("/api/users/me")
                                .header("Authorization", bearer(token)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/logout - Repeated logout returns 401")
        void shouldRejectRepeatedLogout() throws Exception {
                String token = registerAndGetToken();

                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", bearer(token)))
                                .andExpect(status().isNoContent());
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", bearer(token)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Login after logout should issue a new usable token")
        void shouldIssueUsableTokenAfterLoginFollowingLogout() throws Exception {
                String oldToken = registerAndGetToken();
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", bearer(oldToken)))
                                .andExpect(status().isNoContent());

                String loginBody = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                String newToken = objectMapper.readTree(loginBody).path("token").asText();

                assertThat(newToken).isNotEqualTo(oldToken);
                mockMvc.perform(get("/api/users/me")
                                .header("Authorization", bearer(newToken)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PATCH /api/users/me/password - Should change password with old and new values")
        void shouldChangePasswordUsingOldAndNewPassword() throws Exception {
                String token = registerAndGetToken();

                mockMvc.perform(patch("/api/users/me/password")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"oldPassword\":\"password123\",\"newPassword\":\"newPassword123\"}"))
                                .andExpect(status().isNoContent());

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"test@example.com\",\"password\":\"newPassword123\"}"))
                                .andExpect(status().isOk());
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
                                .andExpect(jsonPath("$.address").value("99 ถนนมิตรภาพ"))
                                .andExpect(jsonPath("$.subdistrict").value("ในเมือง"))
                                .andExpect(jsonPath("$.district").value("เมืองขอนแก่น"))
                                .andExpect(jsonPath("$.province").value("ขอนแก่น"))
                                .andExpect(jsonPath("$.postalCode").value("40000"));
        }

        @Test
        @DisplayName("POST /api/auth/logout - Missing and malformed tokens return standard 401")
        void shouldRejectMissingAndMalformedLogoutTokens() throws Exception {
                mockMvc.perform(post("/api/auth/logout"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.error").value("Unauthorized"));

                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer malformed-token"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("POST /api/auth/logout - Expired token returns 401")
        void shouldRejectExpiredLogoutToken() throws Exception {
                ReflectionTestUtils.setField(tokenProvider, "jwtExpiration", -1000L);
                String expiredToken;
                try {
                        expiredToken = tokenProvider.generateToken("expired@example.com");
                } finally {
                        ReflectionTestUtils.setField(tokenProvider, "jwtExpiration", 86400000L);
                }

                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", bearer(expiredToken)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401));
        }

        private String registerAndGetToken() throws Exception {
                String body = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                return objectMapper.readTree(body).path("token").asText();
        }

        private static String bearer(String token) {
                return "Bearer " + token;
        }
}
