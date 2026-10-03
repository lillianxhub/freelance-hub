package th.ac.kku.freelance_hub.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.exception.EmailAlreadyExistsException;
import th.ac.kku.freelance_hub.mapper.UserMapper;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.security.JwtTokenProvider;
import th.ac.kku.freelance_hub.security.EmailNormalizer;
import th.ac.kku.freelance_hub.security.LoginAttemptLimiter;
import th.ac.kku.freelance_hub.exception.LoginRateLimitedException;
import th.ac.kku.freelance_hub.service.AuthService;
import th.ac.kku.freelance_hub.service.AuthSessionResult;
import th.ac.kku.freelance_hub.service.RefreshTokenService;
import th.ac.kku.freelance_hub.exception.InvalidRefreshTokenException;
import th.ac.kku.freelance_hub.dto.request.auth.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest;
import th.ac.kku.freelance_hub.dto.response.auth.AuthResponse;
import th.ac.kku.freelance_hub.dto.response.user.UserResponse;
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        UserProfile profile = userMapper.toProfile(request);
        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthSessionResult login(LoginRequest request, String remoteAddress) {
        String email = EmailNormalizer.normalize(request.getEmail());
        long retryAfter = loginAttemptLimiter.retryAfter(email, remoteAddress);
        if (retryAfter > 0) throw new LoginRateLimitedException(retryAfter);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    email, request.getPassword()));
        } catch (BadCredentialsException ex) {
            loginAttemptLimiter.recordFailure(email, remoteAddress);
            throw ex;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));
        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.issue(user);
        AuthSessionResult session = new AuthSessionResult(createAuthResponse(user),
                refreshToken.value(), refreshToken.expiresAt());
        loginAttemptLimiter.recordSuccess(email);
        return session;
    }

    @Override
    public AuthSessionResult refresh(String refreshToken) {
        AuthSessionResult session = new TransactionTemplate(transactionManager).execute(status -> {
            RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
            // Return normally so replay revocation commits before the 401 is produced.
            if (rotation.token() == null) return null;
            User user = userRepository.findWithProfileById(rotation.user().getId())
                    .orElseThrow(InvalidRefreshTokenException::new);
            return new AuthSessionResult(createAuthResponse(user),
                    rotation.token().value(), rotation.token().expiresAt());
        });
        if (session == null) throw new InvalidRefreshTokenException();
        return session;
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revokeFamily(refreshToken);
    }

    private AuthResponse createAuthResponse(User user) {
        String token = tokenProvider.generateToken(user.getEmail());
        UserResponse userResponse = userMapper.toResponse(user);
        return AuthResponse.of(token, tokenProvider.getExpirationTime(), userResponse);
    }
}
