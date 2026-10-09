package th.ac.kku.freelance_hub.service;

import th.ac.kku.freelance_hub.exception.InvalidStateException;
import th.ac.kku.freelance_hub.exception.InvalidArgumentException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.exception.InvalidCredentialsException;
import th.ac.kku.freelance_hub.mapper.UserMapper;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.dto.request.auth.ChangePasswordRequest;
import th.ac.kku.freelance_hub.dto.request.user.UpdateUserProfileRequest;
import th.ac.kku.freelance_hub.dto.response.user.UserResponse;
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    RefreshTokenService refreshTokenService;

    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userMapper, passwordEncoder, refreshTokenService);
        user = User.builder()
                .id(java.util.UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("old-hash")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changesPasswordOnlyAfterVerifyingOldPassword() {
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        userService.changePassword(ChangePasswordRequest.builder()
                .oldPassword("old-password")
                .newPassword("new-password")
                .build());

        verify(passwordEncoder).encode("new-password");
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllForUser(user.getId());
    }

    @Test
    void readsOnlyTheAuthenticatedUser() {
        UserResponse response = UserResponse.builder().id(user.getId()).email(user.getEmail()).build();
        when(userMapper.toResponse(user)).thenReturn(response);

        assertThat(userService.getCurrentUser()).isSameAs(response);

        verify(userRepository).findByEmail("user@example.com");
        verify(userMapper).toResponse(user);
    }

    @Test
    void updatesTheAuthenticatedUsersProfile() {
        UserProfile profile = UserProfile.builder().displayName("Old name").build();
        user.setProfile(profile);
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .displayName("New name")
                .province("ขอนแก่น")
                .build();
        UserResponse response = UserResponse.builder().id(user.getId()).displayName("New name").build();
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        assertThat(userService.updateCurrentUser(request)).isSameAs(response);

        verify(userMapper).updateProfile(request, profile);
        verify(userRepository).save(user);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void rejectsProfileUpdateWhenProfileIsMissing() {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder().displayName("New name").build();

        assertThatThrownBy(() -> userService.updateCurrentUser(request))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("ไม่พบข้อมูลโปรไฟล์ผู้ใช้");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(userMapper);
    }

    @Test
    void rejectsWrongOldPasswordWithoutSaving() {
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(ChangePasswordRequest.builder()
                .oldPassword("wrong-password")
                .newPassword("new-password")
                .build()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("รหัสผ่านไม่ถูกต้อง");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(refreshTokenService);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(refreshTokenService);
        verifyNoInteractions(userMapper);
    }

    @Test
    void rejectsReusingTheOldPassword() {
        when(passwordEncoder.matches("same-password", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(ChangePasswordRequest.builder()
                .oldPassword("same-password")
                .newPassword("same-password")
                .build()))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("รหัสผ่านใหม่ต้องไม่ซ้ำกับรหัสผ่านเดิม");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(refreshTokenService);
    }
}
