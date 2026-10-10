package th.ac.kku.freelance_hub.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import th.ac.kku.freelance_hub.domain.entity.Project;

@Transactional(propagation = Propagation.MANDATORY)
public class ProjectRepositoryCustomImpl implements ProjectRepositoryCustom {
    private final EntityManager entityManager;

    public ProjectRepositoryCustomImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void refreshForUpdate(Project project) {
        entityManager.refresh(project, LockModeType.PESSIMISTIC_WRITE);
    }
}
