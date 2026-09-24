package th.ac.kku.freelance_hub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.dto.response.UserResponse;
import th.ac.kku.freelance_hub.exception.UserNotFoundException;
import th.ac.kku.freelance_hub.mapper.UserMapper;
import th.ac.kku.freelance_hub.repository.UserRepository;
import java.util.UUID;

/**
 * Service for user operations
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Get current authenticated user
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return userMapper.toResponse(user);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toResponse(user);
    }

    /**
     * Get current authenticated user email from SecurityContext
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        return authentication.getName();
    }

    /**
     * Get current authenticated user entity
     */
    @Transactional(readOnly = true)
    public User getCurrentUserEntity() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}
