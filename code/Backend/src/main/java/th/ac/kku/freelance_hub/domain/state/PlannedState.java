package th.ac.kku.freelance_hub.domain.state;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public final class PlannedState implements ProjectState {

    public static final PlannedState INSTANCE = new PlannedState();

    private PlannedState() {
    }

    @Override
    public ProjectStatus status() {
        return ProjectStatus.PLANNED;
    }

    @Override
    public boolean canTransitionTo(ProjectStatus nextStatus) {
        return nextStatus == ProjectStatus.PLANNED
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