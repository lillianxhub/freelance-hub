package th.ac.kku.freelance_hub.domain.state;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public final class ArchivedState implements ProjectState {

    public static final ArchivedState INSTANCE = new ArchivedState();

    private ArchivedState() {
    }

    @Override
    public ProjectStatus status() {
        return ProjectStatus.ARCHIVED;
    }

    @Override
    public boolean canTransitionTo(ProjectStatus nextStatus) {
        return nextStatus == ProjectStatus.ARCHIVED;
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