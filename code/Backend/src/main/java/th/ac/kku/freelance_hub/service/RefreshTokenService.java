package th.ac.kku.freelance_hub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.kku.freelance_hub.domain.entity.RefreshToken;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.repository.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");
    private final RefreshTokenRepository repository;

    @Value("${app.auth.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public record IssuedToken(String value, Instant expiresAt) { }
    public record Rotation(User user, IssuedToken token, boolean replayed) { }

    @Transactional
    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        return saveToken(user, UUID.randomUUID(), now, now.plusMillis(refreshExpirationMs));
    }

    @Transactional
    public Rotation rotate(String rawToken) {
        if (rawToken == null || !TOKEN_FORMAT.matcher(rawToken).matches()) return new Rotation(null, null, false);
        RefreshToken current = repository.findByTokenHash(hash(rawToken)).orElse(null);
        if (current == null) return new Rotation(null, null, false);
        Instant now = Instant.now();
        if (current.getUsedAt() != null) {
            if (current.getExpiresAt().isAfter(now)) repository.revokeFamily(current.getFamilyId(), now);
            return new Rotation(null, null, true);
        }
        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now)) {
            return new Rotation(null, null, false);
        }
        if (!Boolean.TRUE.equals(current.getUser().getIsActive())) {
            repository.revokeFamily(current.getFamilyId(), now);
            return new Rotation(null, null, false);
        }
        current.setUsedAt(now);
        repository.saveAndFlush(current);
        IssuedToken replacement = saveToken(current.getUser(), current.getFamilyId(), now, current.getExpiresAt());
        return new Rotation(current.getUser(), replacement, false);
    }

    @Transactional
    public void revokeFamily(String rawToken) {
        if (rawToken == null || !TOKEN_FORMAT.matcher(rawToken).matches()) return;
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpired() {
        repository.deleteExpired(Instant.now());
    }

    private IssuedToken saveToken(User user, UUID familyId, Instant createdAt, Instant expiresAt) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setFamilyId(familyId);
        entity.setTokenHash(hash(raw));
        entity.setCreatedAt(createdAt);
        entity.setExpiresAt(expiresAt);
        repository.save(entity);
        return new IssuedToken(raw, expiresAt);
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
