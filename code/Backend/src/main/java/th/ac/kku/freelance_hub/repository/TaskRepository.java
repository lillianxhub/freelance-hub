package th.ac.kku.freelance_hub.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import th.ac.kku.freelance_hub.domain.entity.Task;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndProjectIdAndProjectOwnerId(
            UUID id,
            UUID projectId,
            UUID ownerId
    );

    Page<Task> findAllByProjectIdAndProjectOwnerId(
            UUID projectId,
            UUID ownerId,
            Pageable pageable
    );

    List<Task> findAllByProjectIdAndProjectOwnerIdOrderBySortOrderAsc(
            UUID projectId,
            UUID ownerId
    );

    long countByProjectId(
            UUID projectId
    );
}