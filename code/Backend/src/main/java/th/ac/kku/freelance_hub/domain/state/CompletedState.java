package th.ac.kku.freelance_hub.domain.state;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public final class CompletedState implements ProjectState {

    public static final CompletedState INSTANCE = new CompletedState();

    private CompletedState() {
    }

    @Override
    public ProjectStatus status() {
        return ProjectStatus.COMPLETED;
    }

    @Override
    public boolean canTransitionTo(ProjectStatus nextStatus) {
        return nextStatus == ProjectStatus.COMPLETED
                || nextStatus == ProjectStatus.ARCHIVED;
    }

    @Override
    public boolean canTrackTime() {
        return false;
    }

    @Override
    public boolean canEditTasks() {
        return false;
    }
}