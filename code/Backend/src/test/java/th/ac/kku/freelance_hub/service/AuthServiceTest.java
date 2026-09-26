package th.ac.kku.freelance_hub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.dto.request.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.RegisterRequest;
import th.ac.kku.freelance_hub.dto.response.AuthResponse;
import th.ac.kku.freelance_hub.dto.response.UserResponse;
import th.ac.kku.freelance_hub.exception.EmailAlreadyExistsException;
import th.ac.kku.freelance_hub.mapper.UserMapper;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.security.JwtTokenProvider;
import th.ac.kku.freelance_hub.service.impl.AuthServiceImpl;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtTokenProvider tokenProvider;

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private UserMapper userMapper;

        @Mock
        private RevokedTokenService revokedTokenService;

        @InjectMocks
        private AuthServiceImpl authService;

        private RegisterRequest registerRequest;
        private LoginRequest loginRequest;
        private User user;
        private UserProfile userProfile;
        private UserResponse userResponse;

        @BeforeEach
        void setUp() {
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

                userProfile = UserProfile.builder()
                                .displayName("Test User")
                                .firstName("Test")
                                .lastName("User")
                                .phone("0812345678")
                                .timezone("Asia/Bangkok")
                                .build();

                user = User.builder()
                                .id(UUID.randomUUID())
                                .email("test@example.com")
                                .passwordHash("$2a$10$hashedPassword")
                                .profile(userProfile)
                                .build();

                userResponse = UserResponse.builder()
                                .id(user.getId())
                                .email("test@example.com")
                                .displayName("Test User")
                                .build();
        }

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
                // Given
                when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
                when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$hashedPassword");
                when(userMapper.toProfile(registerRequest)).thenReturn(userProfile);
                when(userRepository.save(any(User.class))).thenReturn(user);
                when(tokenProvider.generateToken(user.getEmail())).thenReturn("jwt-token");
                when(tokenProvider.getExpirationTime()).thenReturn(86400000L);
                when(userMapper.toResponse(user)).thenReturn(userResponse);

                // When
                AuthResponse response = authService.register(registerRequest);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getToken()).isEqualTo("jwt-token");
                assertThat(response.getExpiresIn()).isEqualTo(86400000L);
                assertThat(response.getUser()).isEqualTo(userResponse);

                verify(userRepository).existsByEmail(registerRequest.getEmail());
                verify(passwordEncoder).encode(registerRequest.getPassword());
                verify(userRepository).save(any(User.class));
                verify(tokenProvider).generateToken(user.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
                // Given
                when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

                // When & Then
                assertThatThrownBy(() -> authService.register(registerRequest))
                                .isInstanceOf(EmailAlreadyExistsException.class)
                                .hasMessageContaining(registerRequest.getEmail());

                verify(userRepository).existsByEmail(registerRequest.getEmail());
                verify(userRepository, never()).save(any(User.class));
                verify(tokenProvider, never()).generateToken(anyString());
        }

        @Test
        @DisplayName("Should login user successfully with valid credentials")
        void shouldLoginSuccessfullyWithValidCredentials() {
                // Given
                Authentication authentication = mock(Authentication.class);
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);
                when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
                when(tokenProvider.generateToken(loginRequest.getEmail())).thenReturn("jwt-token");
                when(tokenProvider.getExpirationTime()).thenReturn(86400000L);
                when(userMapper.toResponse(user)).thenReturn(userResponse);

                // When
                AuthResponse response = authService.login(loginRequest);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getToken()).isEqualTo("jwt-token");
                assertThat(response.getExpiresIn()).isEqualTo(86400000L);
                assertThat(response.getUser()).isEqualTo(userResponse);

                verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
                verify(userRepository).findByEmail(loginRequest.getEmail());
                verify(tokenProvider).generateToken(loginRequest.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when login with invalid credentials")
        void shouldThrowExceptionWhenLoginWithInvalidCredentials() {
                // Given
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Invalid credentials"));

                // When & Then
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(BadCredentialsException.class)
                                .hasMessageContaining("Invalid credentials");

                verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
                verify(userRepository, never()).findByEmail(anyString());
                verify(tokenProvider, never()).generateToken(anyString());
        }

        @Test
        @DisplayName("Should throw exception when user not found after authentication")
        void shouldThrowExceptionWhenUserNotFoundAfterAuthentication() {
                // Given
                Authentication authentication = mock(Authentication.class);
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);
                when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("User not found after authentication");

                verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
                verify(userRepository).findByEmail(loginRequest.getEmail());
                verify(tokenProvider, never()).generateToken(anyString());
        }

        @Test
        @DisplayName("Should revoke only the JWT ID during logout")
        void shouldRevokeTokenDuringLogout() {
                String token = "raw-jwt-value";
                String jti = UUID.randomUUID().toString();
                Instant expiresAt = Instant.now().plusSeconds(3600);
                Date expiration = Date.from(expiresAt);
                when(tokenProvider.getEmailFromToken(token)).thenReturn(user.getEmail());
                when(tokenProvider.getJtiFromToken(token)).thenReturn(jti);
                when(tokenProvider.getExpirationFromToken(token)).thenReturn(expiration);
                when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

                authService.logout(token, user.getEmail());

                verify(revokedTokenService).revoke(jti, user, expiration.toInstant());
                verify(revokedTokenService, never()).revoke(eq(token), any(), any());
        }
}
