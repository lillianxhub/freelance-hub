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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import th.ac.kku.freelance_hub.dto.request.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.RegisterRequest;

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

    private ObjectMapper objectMapper = new ObjectMapper();

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

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
}
