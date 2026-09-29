package th.ac.kku.freelance_hub.repository;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface ProjectRepository
        extends JpaRepository<Project, UUID>,
                JpaSpecificationExecutor<Project> {

    @Override
    @EntityGraph(attributePaths = "client")
    Page<Project> findAll(
            Specification<Project> specification,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "client")
    @Query("""
            SELECT p
            FROM Project p
            WHERE p.id = :id
                AND p.owner.id = :ownerId
                AND p.deletedAt IS NULL
            """)
    Optional<Project> findByIdAndOwnerId(
            @Param("id") UUID id,
            @Param("ownerId") UUID ownerId
    );

    boolean existsByIdAndOwnerId(
            UUID id,
            UUID ownerId
    );

    Page<Project> findAllByOwnerId(
            UUID ownerId,
            Pageable pageable
    );

    Page<Project> findAllByOwnerIdAndStatus(
            UUID ownerId,
            ProjectStatus status,
            Pageable pageable
    );

    Page<Project> findAllByOwnerIdAndClientId(
            UUID ownerId,
            UUID clientId,
            Pageable pageable
    );
}