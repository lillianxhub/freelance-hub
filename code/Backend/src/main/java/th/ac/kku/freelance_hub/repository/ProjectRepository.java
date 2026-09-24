package th.ac.kku.freelance_hub.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public interface ProjectRepository
        extends JpaRepository<Project, UUID>,
                JpaSpecificationExecutor<Project> {

    Optional<Project> findByIdAndOwnerId(
            UUID id,
            UUID ownerId
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