package th.ac.kku.freelance_hub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

/**
 * Service for authentication operations (register, login)
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    /**
     * Register new user with profile
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Create user entity
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        // Create and link profile
        UserProfile profile = userMapper.toProfile(request);
        user.setProfile(profile);

        // Save user (cascade will save profile)
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = tokenProvider.generateToken(savedUser.getEmail());

        // Return response
        UserResponse userResponse = userMapper.toResponse(savedUser);
        return AuthResponse.of(token, tokenProvider.getExpirationTime(), userResponse);
    }

    /**
     * Login user and return JWT token
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Authenticate with Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Generate JWT token
        String token = tokenProvider.generateToken(request.getEmail());

        // Get user details
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        // Return response
        UserResponse userResponse = userMapper.toResponse(user);
        return AuthResponse.of(token, tokenProvider.getExpirationTime(), userResponse);
    }
}
