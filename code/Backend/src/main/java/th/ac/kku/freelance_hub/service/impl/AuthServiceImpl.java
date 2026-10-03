package th.ac.kku.freelance_hub.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.exception.EmailAlreadyExistsException;
import th.ac.kku.freelance_hub.mapper.UserMapper;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.security.JwtTokenProvider;
import th.ac.kku.freelance_hub.security.EmailNormalizer;
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

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
        return createAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthSessionResult login(LoginRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));
        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.issue(user);
        return new AuthSessionResult(createAuthResponse(user), refreshToken.value(), refreshToken.expiresAt());
    }

    @Override
    public AuthSessionResult refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        if (rotation.token() == null) throw new InvalidRefreshTokenException();
        User user = userRepository.findWithProfileById(rotation.user().getId())
                .orElseThrow(InvalidRefreshTokenException::new);
        return new AuthSessionResult(createAuthResponse(user),
                rotation.token().value(), rotation.token().expiresAt());
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
