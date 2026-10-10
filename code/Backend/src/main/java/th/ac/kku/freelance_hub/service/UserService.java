package th.ac.kku.freelance_hub.service;

import th.ac.kku.freelance_hub.exception.InvalidStateException;
import th.ac.kku.freelance_hub.exception.AuthenticationRequiredException;
import th.ac.kku.freelance_hub.exception.InvalidArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.exception.InvalidCredentialsException;
import th.ac.kku.freelance_hub.exception.UserNotFoundException;
import th.ac.kku.freelance_hub.mapper.UserMapper;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.RefreshTokenService;
import java.util.UUID;
import th.ac.kku.freelance_hub.dto.request.auth.ChangePasswordRequest;
import th.ac.kku.freelance_hub.dto.request.user.UpdateUserProfileRequest;
import th.ac.kku.freelance_hub.dto.response.user.UserResponse;
/**
 * Service for user operations
 */
@Service
@RequiredArgsConstructor
public class UserService implements CurrentUserProvider {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

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

    /** Update the authenticated user's profile and normalized address. */
    @Transactional
    public UserResponse updateCurrentUser(UpdateUserProfileRequest request) {
        User user = getCurrentUserEntity();
        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new InvalidStateException("ไม่พบข้อมูลโปรไฟล์ผู้ใช้");
        }
        userMapper.updateProfile(request, profile);
        return userMapper.toResponse(userRepository.save(user));
    }

    /** Change the authenticated user's password after verifying the old password. */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserEntity();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("รหัสผ่านไม่ถูกต้อง");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new InvalidArgumentException("รหัสผ่านใหม่ต้องไม่ซ้ำกับรหัสผ่านเดิม");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
    }

    /**
     * Get current authenticated user email from SecurityContext
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationRequiredException();
        }
        return authentication.getName();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID currentUserId() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }

    /**
     * Get current authenticated user entity
     */
    private User getCurrentUserEntity() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}
