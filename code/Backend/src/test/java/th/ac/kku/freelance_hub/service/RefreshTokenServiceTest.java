package th.ac.kku.freelance_hub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import th.ac.kku.freelance_hub.domain.entity.RefreshToken;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.repository.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    private static final long SEVEN_DAYS_MS = 604800000L;
    private static final String RAW_TOKEN = "A".repeat(43);

    @Mock private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(service, "refreshExpirationMs", SEVEN_DAYS_MS);
        user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();
    }

    @Test
    void issueStoresOnlyHashAndSetsSevenDayFamilyExpiry() {
        Instant before = Instant.now();

        RefreshTokenService.IssuedToken issued = service.issue(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        RefreshToken row = saved.getValue();
        assertThat(issued.value()).matches("[A-Za-z0-9_-]{43}");
        assertThat(row.getTokenHash()).isEqualTo(sha256(issued.value())).hasSize(64);
        assertThat(row.getTokenHash()).isNotEqualTo(issued.value());
        assertThat(row.getUser()).isSameAs(user);
        assertThat(row.getFamilyId()).isNotNull();
        assertThat(row.getCreatedAt()).isBetween(before, Instant.now());
        assertThat(row.getExpiresAt()).isEqualTo(row.getCreatedAt().plusMillis(SEVEN_DAYS_MS));
        assertThat(issued.expiresAt()).isEqualTo(row.getExpiresAt());
    }

    @Test
    void rotateMarksOldTokenUsedAndRetainsFamilyAndOriginalExpiry() {
        RefreshToken old = currentToken();
        when(repository.findByTokenHash(sha256(RAW_TOKEN))).thenReturn(Optional.of(old));

        RefreshTokenService.Rotation rotation = service.rotate(RAW_TOKEN);

        assertThat(rotation.replayed()).isFalse();
        assertThat(rotation.user()).isSameAs(user);
        assertThat(rotation.token().value()).isNotEqualTo(RAW_TOKEN);
        assertThat(rotation.token().expiresAt()).isEqualTo(old.getExpiresAt());
        assertThat(old.getUsedAt()).isNotNull();
        verify(repository).saveAndFlush(old);
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getFamilyId()).isEqualTo(old.getFamilyId());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(old.getExpiresAt());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(sha256(rotation.token().value()));
    }

    @Test
    void replayRevokesTheEntireFamilyWithoutIssuingAnotherToken() {
        RefreshToken old = currentToken();
        old.setUsedAt(Instant.now().minusSeconds(10));
        when(repository.findByTokenHash(sha256(RAW_TOKEN))).thenReturn(Optional.of(old));

        RefreshTokenService.Rotation rotation = service.rotate(RAW_TOKEN);

        assertThat(rotation.replayed()).isTrue();
        assertThat(rotation.token()).isNull();
        verify(repository).revokeFamily(eq(old.getFamilyId()), any(Instant.class));
        verify(repository, never()).save(any());
    }

    @Test
    void expiredTokenCannotRotateOrRevokeAnExpiredFamily() {
        RefreshToken old = currentToken();
        old.setExpiresAt(Instant.now().minusSeconds(1));
        when(repository.findByTokenHash(sha256(RAW_TOKEN))).thenReturn(Optional.of(old));

        assertThat(service.rotate(RAW_TOKEN).token()).isNull();

        verify(repository, never()).revokeFamily(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void inactiveUserCannotRotateAndTheirFamilyIsRevoked() {
        RefreshToken old = currentToken();
        user.setIsActive(false);
        when(repository.findByTokenHash(sha256(RAW_TOKEN))).thenReturn(Optional.of(old));

        assertThat(service.rotate(RAW_TOKEN).token()).isNull();

        verify(repository).revokeFamily(eq(old.getFamilyId()), any(Instant.class));
        verify(repository, never()).save(any());
    }

    @Test
    void malformedOrUnknownTokenNeverCreatesARefreshRow() {
        assertThat(service.rotate(null).token()).isNull();
        assertThat(service.rotate("not-a-refresh-token").token()).isNull();
        verifyNoInteractions(repository);

        when(repository.findByTokenHash(sha256(RAW_TOKEN))).thenReturn(Optional.empty());
        assertThat(service.rotate(RAW_TOKEN).token()).isNull();
        verify(repository).findByTokenHash(sha256(RAW_TOKEN));
    }

    @Test
    void logoutRevokesOnlyTheMatchedFamilyAndIgnoresInvalidCookie() {
        service.revokeFamily("invalid");
        verifyNoInteractions(repository);

        RefreshToken old = currentToken();
        when(repository.findByTokenHash(sha256(RAW_TOKEN))).thenReturn(Optional.of(old));
        service.revokeFamily(RAW_TOKEN);

        verify(repository).revokeFamily(eq(old.getFamilyId()), any(Instant.class));
    }

    @Test
    void passwordChangeRevokesAllFamiliesAndCleanupDeletesExpiredRows() {
        service.revokeAllForUser(user.getId());
        service.deleteExpired();

        verify(repository).revokeAllForUser(eq(user.getId()), any(Instant.class));
        verify(repository).deleteExpired(any(Instant.class));
    }

    private RefreshToken currentToken() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setFamilyId(UUID.randomUUID());
        token.setTokenHash(sha256(RAW_TOKEN));
        token.setCreatedAt(Instant.now().minusSeconds(60));
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        return token;
    }

    private static String sha256(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
