package th.ac.kku.freelance_hub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.repository.RefreshTokenRepository;
import th.ac.kku.freelance_hub.security.JwtTokenProvider;
import th.ac.kku.freelance_hub.common.response.RequestTraceFilter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest;
@SpringBootTest
@ActiveProfiles("test")
class UserAuthIntegrationTest {
        private static final String PASSWORD = "password123";
        private static final String TEST_JWT_SECRET = "test-secret-key-for-testing-must-be-at-least-256-bits-long-freelance-hub-test";

        @Autowired
        private WebApplicationContext context;
        @Autowired
        private UserRepository userRepository;
        @Autowired
        private RefreshTokenRepository refreshTokenRepository;
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
        void eachUserCanReadAndUpdateOnlyTheirOwnProfile() throws Exception {
                String firstEmail = uniqueEmail();
                String secondEmail = uniqueEmail();
                String firstToken = register(firstEmail, "First User");
                String secondToken = register(secondEmail, "Second User");

                mockMvc.perform(patch("/api/users/me")
                                .header("Authorization", bearer(firstToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"displayName\":\"Changed First\",\"province\":\"ขอนแก่น\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("อัปเดตข้อมูลผู้ใช้สำเร็จ"))
                                .andExpect(jsonPath("$.data.email").value(firstEmail))
                                .andExpect(jsonPath("$.data.displayName").value("Changed First"));

                mockMvc.perform(get("/api/users/me").header("Authorization", bearer(firstToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("ดึงข้อมูลผู้ใช้สำเร็จ"))
                                .andExpect(jsonPath("$.data.displayName").value("Changed First"))
                                .andExpect(jsonPath("$.data.province").value("ขอนแก่น"));
                mockMvc.perform(get("/api/users/me").header("Authorization", bearer(secondToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.email").value(secondEmail))
                                .andExpect(jsonPath("$.data.displayName").value("Second User"))
                                .andExpect(jsonPath("$.data.province").isEmpty());

        }

        @Test
        void emailIsCanonicalAcrossRegistrationAndLogin() throws Exception {
                String email = "user-" + uniqueEmail();
                String mixedCase = "User-" + email.substring(5);
                assertThat(objectMapper.readValue(objectMapper.writeValueAsString(
                        new Registration("  " + mixedCase + "  ", PASSWORD, "Canonical User")),
                        th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest.class).getEmail()).isEqualTo(email);
                var registration = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new Registration("  " + mixedCase + "  ", PASSWORD, "Canonical User"))))
                                .andReturn().getResponse();
                assertThat(registration.getStatus()).as(registration.getContentAsString()).isEqualTo(201);
                assertThat(userRepository.findByEmail(email)).isPresent();

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(mixedCase, PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.user.email").value(email));

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new Registration(email, PASSWORD, "Duplicate"))))
                                .andExpect(status().isConflict());
        }

        @Test
        void protectedUserEndpointsRejectMissingMalformedAndExpiredAccessTokens() throws Exception {
                String email = uniqueEmail();
                register(email, "Protected User");
                JwtTokenProvider expiredTokenProvider = new JwtTokenProvider();
                ReflectionTestUtils.setField(expiredTokenProvider, "jwtSecret", TEST_JWT_SECRET);
                ReflectionTestUtils.setField(expiredTokenProvider, "jwtExpiration", -1000L);
                String expiredToken = expiredTokenProvider.generateToken(email);

                var unauthorized = mockMvc.perform(get("/api/users/me"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
                                .andExpect(jsonPath("$.error.status").value(401))
                                .andExpect(jsonPath("$.message").value("Authentication is required"))
                                .andReturn();
                assertThat(objectMapper.readTree(unauthorized.getResponse().getContentAsString())
                                .path("error").path("traceId").asText())
                                .isEqualTo(unauthorized.getResponse().getHeader("X-Request-ID"));
                mockMvc.perform(patch("/api/users/me")
                                .header("Authorization", "Bearer malformed")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"displayName\":\"Unauthorized\"}"))
                                .andExpect(status().isUnauthorized());
                mockMvc.perform(patch("/api/users/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"oldPassword\":\"password123\",\"newPassword\":\"anotherPassword123\"}"))
                                .andExpect(status().isUnauthorized());
                mockMvc.perform(get("/api/users/me").header("Authorization", bearer(expiredToken)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void invalidProfileFieldsReturnValidationErrorsWithoutChangingTheProfile() throws Exception {
                String email = uniqueEmail();
                String accessToken = register(email, "Original Name");

                var validation = mockMvc.perform(patch("/api/users/me")
                                .header("Authorization", bearer(accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"displayName\":\"   \",\"phone\":\"letters\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("ข้อมูลที่ส่งมาไม่ถูกต้อง"))
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.error.fieldErrors.displayName").value("ชื่อที่แสดงต้องไม่เป็นช่องว่าง"))
                                .andExpect(jsonPath("$.error.fieldErrors.phone").value(
                                                "เบอร์โทรศัพท์ต้องมีเฉพาะตัวเลขหรือเครื่องหมายคั่นที่ใช้ทั่วไป"))
                                .andExpect(jsonPath("$.error.details").value(org.hamcrest.Matchers.nullValue()))
                                .andExpect(jsonPath("$.error.status").value(400))
                                .andReturn();
                var errorBody = objectMapper.readTree(validation.getResponse().getContentAsString());
                assertThat(errorBody.path("error").path("traceId").asText())
                                .isEqualTo(validation.getResponse().getHeader("X-Request-ID"));
                assertThat(java.time.Instant.parse(errorBody.path("error").path("timestamp").asText()))
                                .isNotNull();

                mockMvc.perform(get("/api/users/me").header("Authorization", bearer(accessToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.displayName").value("Original Name"));
        }

        @Test
        void passwordChangeRequiresOldPasswordAndRevokesEveryRefreshFamily() throws Exception {
                String email = uniqueEmail();
                String accessToken = register(email, "Password User");
                Cookie firstFamily = loginCookie(email, PASSWORD);
                Cookie secondFamily = loginCookie(email, PASSWORD);

                mockMvc.perform(patch("/api/users/me/password")
                                .header("Authorization", bearer(accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"oldPassword\":\"wrong-password\",\"newPassword\":\"newPassword123\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("รหัสผ่านไม่ถูกต้อง"));

                mockMvc.perform(patch("/api/users/me/password")
                                .header("Authorization", bearer(accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"oldPassword\":\"password123\",\"newPassword\":\"newPassword123\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("เปลี่ยนรหัสผ่านสำเร็จ"));

                mockMvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173").cookie(firstFamily))
                                .andExpect(status().isUnauthorized());
                mockMvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173").cookie(secondFamily))
                                .andExpect(status().isUnauthorized());
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(email, PASSWORD)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("อีเมลหรือรหัสผ่านไม่ถูกต้อง"));
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(email, "newPassword123")))
                                .andExpect(status().isOk());
                // Access JWTs already issued remain usable until their 15-minute expiry.
                mockMvc.perform(get("/api/users/me").header("Authorization", bearer(accessToken)))
                                .andExpect(status().isOk());
        }

        @Test
        void inactiveUserCannotUseExistingAccessOrRefreshToken() throws Exception {
                String email = uniqueEmail();
                String accessToken = register(email, "Inactive User");
                Cookie refreshCookie = loginCookie(email, PASSWORD);
                User user = userRepository.findByEmail(email).orElseThrow();
                user.setIsActive(false);
                userRepository.saveAndFlush(user);

                mockMvc.perform(get("/api/users/me").header("Authorization", bearer(accessToken)))
                                .andExpect(status().isUnauthorized());
                mockMvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173").cookie(refreshCookie))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void refreshReplayRevokesTheCommittedFamilyAndLogoutRevokesAnotherFamily() throws Exception {
                String email = uniqueEmail();
                register(email, "Refresh User");
                UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
                Cookie original = loginCookie(email, PASSWORD);

                String rotatedHeader = mockMvc.perform(post("/api/auth/refresh")
                                .header("Origin", "http://localhost:5173").cookie(original))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getHeader("Set-Cookie");
                Cookie rotated = new Cookie("fh_refresh", rotatedHeader.split(";", 2)[0].split("=", 2)[1]);
                assertThat(refreshTokenRepository.findAll().stream()
                                .filter(token -> token.getUser().getId().equals(userId))
                                .filter(token -> token.getUsedAt() != null).count()).isEqualTo(1);
                UUID familyId = refreshTokenRepository.findAll().stream()
                                .filter(token -> token.getUser().getId().equals(userId))
                                .filter(token -> token.getUsedAt() != null)
                                .findFirst().orElseThrow().getFamilyId();

                mockMvc.perform(post("/api/auth/refresh")
                                .header("Origin", "http://localhost:5173").cookie(original))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
                // The replay response is 401, but its family revocation must still commit.
                assertThat(refreshTokenRepository.findAll().stream()
                                .filter(token -> token.getUser().getId().equals(userId))
                                .filter(token -> token.getFamilyId().equals(familyId))
                                .allMatch(token -> token.getRevokedAt() != null)).isTrue();
                mockMvc.perform(post("/api/auth/refresh")
                                .header("Origin", "http://localhost:5173").cookie(rotated))
                                .andExpect(status().isUnauthorized());

                Cookie logoutFamily = loginCookie(email, PASSWORD);
                mockMvc.perform(post("/api/auth/logout")
                                .header("Origin", "http://localhost:5173").cookie(logoutFamily))
                                .andExpect(status().isNoContent());
                mockMvc.perform(post("/api/auth/refresh")
                                .header("Origin", "http://localhost:5173").cookie(logoutFamily))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void forbiddenMissingAndDuplicateRequestsUseTheSameErrorContract() throws Exception {
                String email = uniqueEmail();
                String token = register(email, "Contract User");
                Cookie cookie = loginCookie(email, PASSWORD);

                var forbidden = mockMvc.perform(post("/api/auth/refresh").cookie(cookie))
                                .andExpect(status().isForbidden()).andReturn().getResponse();
                assertErrorContract(forbidden, 403, "ORIGIN_NOT_ALLOWED");

                var missing = mockMvc.perform(get("/api/no-such-resource")
                                .header("Authorization", bearer(token)))
                                .andExpect(status().isNotFound()).andReturn().getResponse();
                assertErrorContract(missing, 404, "NOT_FOUND");

                var duplicate = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new Registration(email, PASSWORD, "Duplicate"))))
                                .andExpect(status().isConflict()).andReturn().getResponse();
                assertErrorContract(duplicate, 409, "EMAIL_ALREADY_EXISTS");
        }

        private void assertErrorContract(MockHttpServletResponse response, int status, String code) throws Exception {
                JsonNode body = objectMapper.readTree(response.getContentAsString());
                assertThat(body.path("success").asBoolean()).isFalse();
                assertThat(body.path("message").asText()).isNotBlank();
                assertThat(body.path("error").path("code").asText()).isEqualTo(code);
                assertThat(body.path("error").path("status").asInt()).isEqualTo(status);
                assertThat(body.path("error").path("traceId").asText())
                                .isEqualTo(response.getHeader("X-Request-ID"));
                java.time.Instant.parse(body.path("error").path("timestamp").asText());
        }

        private String register(String email, String displayName) throws Exception {
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(new Registration(email, PASSWORD, displayName))))
                                .andExpect(status().isCreated());
                String body = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(email, PASSWORD)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                return objectMapper.readTree(body).path("data").path("token").asText();
        }

        private Cookie loginCookie(String email, String password) throws Exception {
                String setCookie = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(email, password)))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Set-Cookie", allOf(
                                                containsString("HttpOnly"), containsString("SameSite=Lax"),
                                                containsString("Path=/api/auth"))))
                                .andReturn().getResponse().getHeader("Set-Cookie");
                return new Cookie("fh_refresh", setCookie.split(";", 2)[0].split("=", 2)[1]);
        }

        private String loginJson(String email, String password) throws Exception {
                return objectMapper.writeValueAsString(new Login(email, password));
        }

        private static String bearer(String token) {
                return "Bearer " + token;
        }

        private static String uniqueEmail() {
                return UUID.randomUUID() + "@example.com";
        }

        private record Registration(String email, String password, String displayName) {
        }

        private record Login(String email, String password) {
        }
}
