package th.ac.kku.freelance_hub.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import th.ac.kku.freelance_hub.domain.entity.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {

    boolean existsByJti(String jti);

    Optional<RevokedToken> findByJti(String jti);
}
