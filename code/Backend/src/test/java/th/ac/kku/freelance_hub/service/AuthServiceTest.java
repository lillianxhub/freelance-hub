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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.exception.EmailAlreadyExistsException;
import th.ac.kku.freelance_hub.exception.InvalidRefreshTokenException;
import th.ac.kku.freelance_hub.mapper.UserMapper;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.security.JwtTokenProvider;
import th.ac.kku.freelance_hub.security.LoginAttemptLimiter;
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
import th.ac.kku.freelance_hub.dto.request.auth.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest;
import th.ac.kku.freelance_hub.dto.response.auth.AuthResponse;
import th.ac.kku.freelance_hub.dto.response.user.UserResponse;
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
        private RefreshTokenService refreshTokenService;

        @Mock
        private LoginAttemptLimiter loginAttemptLimiter;

        @Mock
        private PlatformTransactionManager transactionManager;

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
                when(userMapper.toResponse(user)).thenReturn(userResponse);

                when(tokenProvider.generateToken(user.getEmail())).thenReturn("jwt-token");
                when(tokenProvider.getExpirationTime()).thenReturn(900000L);
                when(refreshTokenService.issue(user)).thenReturn(
                                new RefreshTokenService.IssuedToken("refresh-token", Instant.now().plusSeconds(3600)));

                // When
                AuthSessionResult response = authService.register(registerRequest);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.response().getUser()).isEqualTo(userResponse);

                verify(userRepository).existsByEmail(registerRequest.getEmail());
                verify(passwordEncoder).encode(registerRequest.getPassword());
                verify(userRepository).save(any(User.class));
                verify(refreshTokenService).issue(user);
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
                // Given
                when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

                // When & Then
                assertThatThrownBy(() -> authService.register(registerRequest))
                                .isInstanceOf(EmailAlreadyExistsException.class)
                                .hasMessage("อีเมลนี้ถูกใช้งานแล้ว");

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
                when(tokenProvider.getExpirationTime()).thenReturn(900000L);
                when(userMapper.toResponse(user)).thenReturn(userResponse);
                when(refreshTokenService.issue(user)).thenReturn(
                        new RefreshTokenService.IssuedToken("refresh-token", Instant.now().plusSeconds(604800)));

                // When
                AuthSessionResult session = authService.login(loginRequest, "127.0.0.1");
                AuthResponse response = session.response();

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getToken()).isEqualTo("jwt-token");
                assertThat(response.getExpiresIn()).isEqualTo(900000L);
                assertThat(response.getUser()).isEqualTo(userResponse);
                assertThat(session.refreshToken()).isEqualTo("refresh-token");

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
                assertThatThrownBy(() -> authService.login(loginRequest, "127.0.0.1"))
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
                assertThatThrownBy(() -> authService.login(loginRequest, "127.0.0.1"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("User not found after authentication");

                verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
                verify(userRepository).findByEmail(loginRequest.getEmail());
                verify(tokenProvider, never()).generateToken(anyString());
        }

        @Test
        @DisplayName("Should revoke only the current refresh-token family during logout")
        void shouldRevokeRefreshFamilyDuringLogout() {
                authService.logout("refresh-token");
                verify(refreshTokenService).revokeFamily("refresh-token");
        }

        @Test
        @DisplayName("Refresh issues a new access token for the rotated user")
        void shouldRefreshSessionForRotatedUser() {
                when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
                Instant expiresAt = Instant.now().plusSeconds(3600);
                when(refreshTokenService.rotate("old-refresh")).thenReturn(new RefreshTokenService.Rotation(
                                user, new RefreshTokenService.IssuedToken("new-refresh", expiresAt), false));
                when(userRepository.findWithProfileById(user.getId())).thenReturn(Optional.of(user));
                when(tokenProvider.generateToken(user.getEmail())).thenReturn("new-access");
                when(tokenProvider.getExpirationTime()).thenReturn(900000L);
                when(userMapper.toResponse(user)).thenReturn(userResponse);

                AuthSessionResult session = authService.refresh("old-refresh");

                assertThat(session.response().getToken()).isEqualTo("new-access");
                assertThat(session.response().getUser()).isEqualTo(userResponse);
                assertThat(session.refreshToken()).isEqualTo("new-refresh");
                assertThat(session.refreshExpiresAt()).isEqualTo(expiresAt);
                verify(userRepository).findWithProfileById(user.getId());
        }

        @Test
        @DisplayName("Invalid or replayed refresh token cannot issue an access token")
        void shouldRejectInvalidRefreshRotation() {
                when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
                when(refreshTokenService.rotate("replayed-refresh"))
                                .thenReturn(new RefreshTokenService.Rotation(null, null, true));

                assertThatThrownBy(() -> authService.refresh("replayed-refresh"))
                                .isInstanceOf(InvalidRefreshTokenException.class);

                verify(transactionManager).commit(any());
                verify(transactionManager, never()).rollback(any());
                verifyNoInteractions(tokenProvider);
                verify(userRepository, never()).findWithProfileById(any());
        }

        @Test
        @DisplayName("Refresh cannot issue an access token if the user no longer exists")
        void shouldRejectRefreshForMissingUser() {
                when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
                when(refreshTokenService.rotate("old-refresh")).thenReturn(new RefreshTokenService.Rotation(
                                user, new RefreshTokenService.IssuedToken("new-refresh", Instant.now().plusSeconds(3600)),
                                false));
                when(userRepository.findWithProfileById(user.getId())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> authService.refresh("old-refresh"))
                                .isInstanceOf(InvalidRefreshTokenException.class);

                verify(transactionManager).rollback(any());
                verifyNoInteractions(tokenProvider);
        }
}
