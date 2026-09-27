package th.ac.kku.freelance_hub.domain.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import th.ac.kku.freelance_hub.domain.state.ProjectState;
import th.ac.kku.freelance_hub.domain.state.ProjectStates;

@Entity
@Table(
        name = "projects",
        indexes = {
                @Index(
                        name = "idx_projects_owner_status",
                        columnList = "owner_id,status"
                ),
                @Index(
                        name = "idx_projects_client_status",
                        columnList = "client_id,status"
                ),
                @Index(
                        name = "idx_projects_owner_start_date",
                        columnList = "owner_id,start_date"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_projects_id_owner",
                        columnNames = {"id", "owner_id"}
                )
        }
)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 7)
    private String color;



    @Column(name = "target_minutes")
    private Integer targetMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.PLANNED;

    @OneToMany(
            mappedBy = "project",
            fetch = FetchType.LAZY
    )
    private List<Task> tasks = new ArrayList<>();

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Project() {
    }

    public Project(
            User owner,
            Client client,
            String name
    ) {
        this.owner = Objects.requireNonNull(
                owner,
                "owner is required"
        );

        this.client = Objects.requireNonNull(
                client,
                "client is required"
        );

        this.name = requireName(name);

    }

    public void updateDetails(
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            String color,
            Integer targetMinutes
    ) {
        validateDates(startDate, endDate);
        validateColor(color);
        validateTargetMinutes(targetMinutes);

        this.name = requireName(name);
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.color = color;
        this.targetMinutes = targetMinutes;
    }

    public void changeClient(Client client) {
        this.client = Objects.requireNonNull(
                client,
                "client is required"
        );
    }

    public void changeStatus(ProjectStatus nextStatus) {
        Objects.requireNonNull(nextStatus, "nextStatus is required");

        ProjectState currentState = ProjectStates.from(status);
        if (!currentState.canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    "cannot change project status from "
                            + status
                            + " to "
                            + nextStatus
            );
        }

        status = nextStatus;
    }

    public void archive() {
        changeStatus(ProjectStatus.ARCHIVED);
    }

    public boolean canTrackTime() {
        return ProjectStates.from(status).canTrackTime();
    }

    public boolean canEditTasks() {
        return ProjectStates.from(status).canEditTasks();
    }

    public BigDecimal progress(int trackedMinutes) {
        if (trackedMinutes < 0) {
            throw new IllegalArgumentException(
                    "trackedMinutes must not be negative"
            );
        }

        if (targetMinutes == null || targetMinutes == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(trackedMinutes)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(targetMinutes),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    void addTask(Task task) {
        tasks.add(
                Objects.requireNonNull(
                        task,
                        "task is required"
                )
        );
    }


    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "name is required"
            );
        }

        return name.trim();
    }

    private static void validateDates(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate != null
                && endDate != null
                && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "endDate must not be before startDate"
            );
        }
    }

    private static void validateColor(String color) {
        if (color != null
                && !color.matches("#[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException(
                    "color must use #RRGGBB format"
            );
        }
    }

    private static void validateTargetMinutes(
            Integer targetMinutes
    ) {
        if (targetMinutes != null && targetMinutes <= 0) {
            throw new IllegalArgumentException(
                    "targetMinutes must be greater than zero"
            );
        }
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

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public Client getClient() {
        return client;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getColor() {
        return color;
    }


    public Integer getTargetMinutes() {
        return targetMinutes;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
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