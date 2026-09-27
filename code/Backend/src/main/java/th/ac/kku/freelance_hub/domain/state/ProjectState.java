package th.ac.kku.freelance_hub.domain.state;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public interface ProjectState {

    ProjectStatus status();

    boolean canTransitionTo(ProjectStatus nextStatus);

    boolean canTrackTime();

    boolean canEditTasks();
}