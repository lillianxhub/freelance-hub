package th.ac.kku.freelance_hub.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import th.ac.kku.freelance_hub.service.AuthService;
import th.ac.kku.freelance_hub.service.RevokedTokenService;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final RevokedTokenService revokedTokenService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        UserProfile profile = userMapper.toProfile(request);
        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        return createAuthResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));
        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(String token, String authenticatedEmail) {
        String tokenEmail = tokenProvider.getEmailFromToken(token);
        if (!authenticatedEmail.equals(tokenEmail)) {
            throw new IllegalArgumentException("Token subject does not match authenticated user");
        }

        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));
        revokedTokenService.revoke(
                tokenProvider.getJtiFromToken(token),
                user,
                tokenProvider.getExpirationFromToken(token).toInstant()
        );
    }

    private AuthResponse createAuthResponse(User user) {
        String token = tokenProvider.generateToken(user.getEmail());
        UserResponse userResponse = userMapper.toResponse(user);
        return AuthResponse.of(token, tokenProvider.getExpirationTime(), userResponse);
    }
}
