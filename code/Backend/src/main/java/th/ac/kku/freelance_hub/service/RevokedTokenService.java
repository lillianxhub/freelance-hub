package th.ac.kku.freelance_hub.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.kku.freelance_hub.domain.entity.RevokedToken;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.repository.RevokedTokenRepository;

@Service
@RequiredArgsConstructor
public class RevokedTokenService {

    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return revokedTokenRepository.existsByJti(jti);
    }

    @Transactional
    public void revoke(String jti, User user, Instant expiresAt) {
        if (!revokedTokenRepository.existsByJti(jti)) {
            revokedTokenRepository.save(
                    new RevokedToken(jti, user, expiresAt, Instant.now())
            );
        }
    }
}
