package th.ac.kku.freelance_hub.repository;

import th.ac.kku.freelance_hub.domain.entity.Project;

public interface ProjectRepositoryCustom {
    /** Reload and lock a managed project within the caller's transaction. */
    void refreshForUpdate(Project project);
}
