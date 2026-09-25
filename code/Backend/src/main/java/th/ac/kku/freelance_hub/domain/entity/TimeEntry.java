package th.ac.kku.freelance_hub.domain.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import th.ac.kku.freelance_hub.domain.enums.EntryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A period of work recorded against a project and, optionally, a task.
 *
 * <p>A timer entry is created without an end time and duration. A manual entry
 * is completed immediately. All timestamps are represented by {@link Instant}
 * so they can be stored in UTC.</p>
 */
@Entity
@Table(
        name = "time_entries",
        indexes = {
                @Index(
                        name = "idx_time_entries_owner_started_at",
                        columnList = "owner_id,started_at"
                ),
                @Index(
                        name = "idx_time_entries_project_started_at",
                        columnList = "project_id,started_at"
                ),
                @Index(
                        name = "idx_time_entries_task_started_at",
                        columnList = "task_id,started_at"
                )
        }
)
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected TimeEntry() {
    }

    private TimeEntry(
            User owner,
            Project project,
            Task task,
            String description,
            EntryType entryType,
            Instant startedAt
    ) {
        this.owner = Objects.requireNonNull(owner, "owner is required");
        this.project = requireOwnedProject(owner, project);
        this.task = requireTaskInProject(task, project);
        this.description = trimToNull(description);
        this.entryType = Objects.requireNonNull(entryType, "entryType is required");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt is required");
    }

    /** Creates a running timer entry. */
    public static TimeEntry startTimer(
            User owner,
            Project project,
            Task task,
            String description,
            Instant startedAt
    ) {
        requireTrackableProject(owner, project);
        return new TimeEntry(
                owner,
                project,
                task,
                description,
                EntryType.TIMER,
                startedAt
        );
    }

    /** Creates a completed manual entry and calculates its duration. */
    public static TimeEntry createManual(
            User owner,
            Project project,
            Task task,
            String description,
            Instant startedAt,
            Instant endedAt
    ) {
        TimeEntry entry = new TimeEntry(
                owner,
                project,
                task,
                description,
                EntryType.MANUAL,
                startedAt
        );
        entry.completeAt(endedAt);
        return entry;
    }

    /** Creates a completed manual entry with a specified duration. */
    public static TimeEntry createManualWithDuration(
            User owner,
            Project project,
            Task task,
            String description,
            Instant startedAt,
            int durationMinutes
    ) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "durationMinutes must be greater than zero"
            );
        }

        Instant requiredStart = Objects.requireNonNull(startedAt,"startedAt is required");
        Instant endedAt = requiredStart.plusSeconds(
                Math.multiplyExact((long) durationMinutes, 60)
        );

        return createManual(
                owner,
                project,
                task,
                description,
                requiredStart,
                endedAt
        );
    }


    /** Stops a running timer and records its calculated duration. */
    public void stop(Instant endedAt) {
        requireUnlocked();
        if (entryType != EntryType.TIMER) {
            throw new IllegalStateException("only a timer entry can be stopped");
        }
        if (!isRunning()) {
            throw new IllegalStateException("timer is not running");
        }
        completeAt(endedAt);
    }

    /** Updates the work context without changing the recorded time range. */
    public void updateDetails(
            Project project,
            Task task,
            String description
    ) {
        requireUnlocked();
        this.project = requireOwnedProject(owner, project);
        this.task = requireTaskInProject(task, project);
        this.description = trimToNull(description);
    }

    /** Updates the time range of a completed entry. */
    public void updateTimeRange(Instant startedAt, Instant endedAt) {
        requireUnlocked();
        if (isRunning()) {
            throw new IllegalStateException("a running timer cannot be edited");
        }
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt is required");
        completeAt(endedAt);
    }

    /** Locks a completed entry so it can no longer be edited. */
    public void lock(Instant lockedAt) {
        if (isRunning()) {
            throw new IllegalStateException("a running timer cannot be locked");
        }
        if (isLocked()) {
            throw new IllegalStateException("time entry is already locked");
        }
        this.lockedAt = Objects.requireNonNull(lockedAt, "lockedAt is required");
    }

    public boolean isRunning() {
        return entryType == EntryType.TIMER
                && endedAt == null
                && durationMinutes == null;
    }

    public boolean isLocked() {
        return lockedAt != null;
    }

    private void completeAt(Instant endedAt) {
        Instant requiredEnd = Objects.requireNonNull(endedAt, "endedAt is required");
        if (!requiredEnd.isAfter(startedAt)) {
            throw new IllegalArgumentException("endedAt must be after startedAt");
        }

        long elapsedSeconds = Duration.between(startedAt, requiredEnd).getSeconds();
        long roundedMinutes = Math.addExact(elapsedSeconds, 59) / 60;

        this.endedAt = requiredEnd;
        this.durationMinutes = Math.toIntExact(roundedMinutes);
    }

    private void requireUnlocked() {
        if (isLocked()) {
            throw new IllegalStateException("locked time entry cannot be changed");
        }
    }

    private static Project requireTrackableProject(User owner, Project project) {
        Project requiredProject = requireOwnedProject(owner, project);
        if (!requiredProject.canTrackTime()) {
            throw new IllegalStateException("project must be active to track time");
        }
        return requiredProject;
    }

    private static Project requireOwnedProject(User owner, Project project) {
        Project requiredProject = Objects.requireNonNull(project, "project is required");
        if (!sameUser(owner, requiredProject.getOwner())) {
            throw new IllegalArgumentException("project must belong to owner");
        }
        return requiredProject;
    }

    private static Task requireTaskInProject(Task task, Project project) {
        if (task == null) {
            return null;
        }
        Project taskProject = task.getProject();
        boolean sameInstance = taskProject == project;
        boolean samePersistentId = taskProject.getId() != null
                && taskProject.getId().equals(project.getId());
        if (!sameInstance && !samePersistentId) {
            throw new IllegalArgumentException("task must belong to project");
        }
        return task;
    }

    private static boolean sameUser(User first, User second) {
        if (first == second) {
            return true;
        }
        return first.getId() != null && first.getId().equals(second.getId());
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    //getters

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public Project getProject() {
        return project;
    }

    public Task getTask() {
        return task;
    }

    public String getDescription() {
        return description;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
