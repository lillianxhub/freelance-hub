package th.ac.kku.freelance_hub.domain.state;


import java.util.Objects;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public final class ProjectStates {

    private ProjectStates() {
    }

    public static ProjectState from(ProjectStatus status) {
        Objects.requireNonNull(status, "status is required");

        return switch (status) {
            case PLANNED -> PlannedState.INSTANCE;
            case ACTIVE -> ActiveState.INSTANCE;
            case ON_HOLD -> OnHoldState.INSTANCE;
            case COMPLETED -> CompletedState.INSTANCE;
            case ARCHIVED -> ArchivedState.INSTANCE;
        };
    }
}   