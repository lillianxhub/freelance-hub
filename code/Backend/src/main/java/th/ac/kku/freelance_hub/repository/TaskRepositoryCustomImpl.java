package th.ac.kku.freelance_hub.repository;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import th.ac.kku.freelance_hub.domain.entity.Task;

@Transactional(propagation = Propagation.MANDATORY)
public class TaskRepositoryCustomImpl implements TaskRepositoryCustom {
    private final EntityManager entityManager;

    public TaskRepositoryCustomImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void refresh(Task task) {
        entityManager.refresh(task);
    }
}
