package th.ac.kku.freelance_hub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.repository.UserRepository;

class CustomUserDetailsServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(repository);

    @Test
    void activeUserKeepsPasswordAndRoleWhileInactiveUserIsDisabled() {
        User user = User.builder().email("owner@example.com").passwordHash("hash").build();
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var details = service.loadUserByUsername(user.getEmail());
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting(authority -> authority.getAuthority()).containsExactly("ROLE_USER");
        user.setIsActive(false);
        assertThat(service.loadUserByUsername(user.getEmail()).isEnabled()).isFalse();
        user.setIsActive(null);
        assertThat(service.loadUserByUsername(user.getEmail()).isEnabled()).isFalse();
    }

    @Test
    void unknownEmailCannotAuthenticate() {
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void emailNormalizationIsIndependentOfDefaultLocale() {
        var previous = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            assertThat(EmailNormalizer.normalize("  INFO@EXAMPLE.COM  ")).isEqualTo("info@example.com");
        } finally { java.util.Locale.setDefault(previous); }
    }
}
