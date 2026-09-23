package th.ac.kku.freelance_hub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for clients.
 *
 * <p>Every business-data query includes {@code ownerId} to prevent one user
 * from reading or modifying another user's clients.</p>
 */
public interface ClientRepository
    extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {

    Optional<Client> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Client> findAllByOwnerId(UUID ownerId, Pageable pageable);

    Page<Client> findAllByOwnerIdAndStatus(
        UUID ownerId,
        ClientStatus status,
        Pageable pageable
    );
}
