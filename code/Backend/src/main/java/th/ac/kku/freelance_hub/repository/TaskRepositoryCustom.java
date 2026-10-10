package th.ac.kku.freelance_hub.repository;

import th.ac.kku.freelance_hub.domain.entity.Task;

public interface TaskRepositoryCustom {
    /** Reload a managed task within the caller's transaction. */
    void refresh(Task task);
}
