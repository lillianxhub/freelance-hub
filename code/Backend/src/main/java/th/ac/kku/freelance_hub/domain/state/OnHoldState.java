package th.ac.kku.freelance_hub.domain.state;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public final class OnHoldState implements ProjectState {

    public static final OnHoldState INSTANCE = new OnHoldState();

    private OnHoldState() {
    }

    @Override
    public ProjectStatus status() {
        return ProjectStatus.ON_HOLD;
    }

    @Override
    public boolean canTransitionTo(ProjectStatus nextStatus) {
        return nextStatus == ProjectStatus.ON_HOLD
                || nextStatus == ProjectStatus.ACTIVE
                || nextStatus == ProjectStatus.ARCHIVED;
    }

    @Override
    public boolean canTrackTime() {
        return false;
    }

    @Override
    public boolean canEditTasks() {
        return true;
    }
}