package th.ac.kku.freelance_hub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;

/**
 * Repository interface for UserProfile entity
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
