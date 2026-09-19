package th.ac.kku.freelance_hub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import th.ac.kku.freelance_hub.domain.entity.Client;

import java.util.Optional;

/**
 * Data access for clients.
 *
 * <p>Every business-data query includes {@code ownerId} to prevent one user
 * from reading or modifying another user's clients.</p>
 */
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    /**
     * Returns clients owned by a user with optional active-status and text filters.
     * The text filter searches the fields used most often on the client list page.
     */
    @Query("""
        SELECT client
        FROM Client client
        WHERE client.owner.id = :ownerId
          AND (:active IS NULL OR client.active = :active)
          AND (
                :search IS NULL
                OR :search = ''
                OR LOWER(client.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(client.companyName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(client.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR client.phone LIKE CONCAT('%', :search, '%')
              )
        """)
    Page<Client> searchByOwner(
        @Param("ownerId") Long ownerId,
        @Param("active") Boolean active,
        @Param("search") String search,
        Pageable pageable
    );
}
