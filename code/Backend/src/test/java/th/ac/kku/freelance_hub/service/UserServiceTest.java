package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import th.ac.kku.freelance_hub.exception.InvalidCredentialsException;
import th.ac.kku.freelance_hub.mapper.UserMapper;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.dto.request.ChangePasswordRequest;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;

    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userMapper, passwordEncoder);
        user = User.builder()
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
    }

    @Test
    void rejectsWrongOldPasswordWithoutSaving() {
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(ChangePasswordRequest.builder()
            .oldPassword("wrong-password")
            .newPassword("new-password")
            .build()))
            .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(userMapper);
    }

    @Test
    void rejectsReusingTheOldPassword() {
        when(passwordEncoder.matches("same-password", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(ChangePasswordRequest.builder()
            .oldPassword("same-password")
            .newPassword("same-password")
            .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("differ");
    }
}
