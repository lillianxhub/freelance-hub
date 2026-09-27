package th.ac.kku.freelance_hub.domain.state;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public final class ActiveState implements ProjectState {

    public static final ActiveState INSTANCE = new ActiveState();

    private ActiveState() {
    }

    @Override
    public ProjectStatus status() {
        return ProjectStatus.ACTIVE;
    }

    @Override
    public boolean canTransitionTo(ProjectStatus nextStatus) {
        return nextStatus == ProjectStatus.ACTIVE
                || nextStatus == ProjectStatus.ON_HOLD
                || nextStatus == ProjectStatus.COMPLETED
                || nextStatus == ProjectStatus.ARCHIVED;
    }

    @Override
    public boolean canTrackTime() {
        return true;
    }

    @Override
    public boolean canEditTasks() {
        return true;
    }
}