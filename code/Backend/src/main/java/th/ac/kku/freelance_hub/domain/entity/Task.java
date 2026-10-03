package th.ac.kku.freelance_hub.domain.entity;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(
                        name = "idx_tasks_project_status",
                        columnList = "project_id,status"
                ),
                @Index(
                        name = "idx_tasks_project_is_active",
                        columnList = "project_id,is_active"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tasks_project_sort_order",
                        columnNames = {
                                "project_id",
                                "sort_order"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_tasks_id_project",
                        columnNames = {
                                "id",
                                "project_id"
                        }
                )
        }
)
public class Task {

    //Attributes
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    //Task หลายตัวสามารถอยู่ใน Project เดียวกันได้
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "project_id",
            nullable = false
    )
    private Project project;

    @Column(
            nullable = false,
            length = 180
    )
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private TaskStatus status = TaskStatus.OPEN;

    //เลขลำดับของ Task ใน Project เอาไว้ใช้ในการจัดเรียง Task ใน Project
    @Column(
            name = "sort_order",
            nullable = false
    )
    private int sortOrder;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

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

    //เอาไว้ใช้ในการจัดการ concurrent update ของ Task
    @Version
    @Column(nullable = false)
    private Long version;



    protected Task() {
    }

    public Task(
            Project project,
            String name,
            int sortOrder
    ) {
        this.project = Objects.requireNonNull(
                project,
                "project is required"
        );

        this.name = requireName(name);

        reorder(sortOrder);

        project.addTask(this);
    }

    public void updateDetails(
            String name,
            String description
    ) {
        this.name = requireName(name);
        this.description = description;
    }

    // Allow .OPEN ONly
    public void start() {
        if (status != TaskStatus.OPEN) {
            throw new IllegalStateException(
                    "only an open task can be started"
            );
        }

        status = TaskStatus.IN_PROGRESS;
    }

    public void complete(Instant completedAt) {
        if (status == TaskStatus.COMPLETED) {
            throw new IllegalStateException(
                    "task is already completed"
            );
        }

        this.completedAt = Objects.requireNonNull(
                completedAt,
                "completedAt is required"
        );

        status = TaskStatus.COMPLETED;
    }

    public void changeStatus(TaskStatus nextStatus, Instant completedAt) {
        Objects.requireNonNull(nextStatus, "nextStatus is required");
        if (status == nextStatus) {
            return;
        }

        switch (nextStatus) {
            case OPEN -> throw new IllegalStateException(
                    "cannot change task status from " + status + " to OPEN"
            );
            case IN_PROGRESS -> {
                if (status == TaskStatus.COMPLETED) {
                    this.completedAt = null;
                    status = TaskStatus.IN_PROGRESS;
                } else {
                    start();
                }
            }
            case COMPLETED -> complete(completedAt);
        }
    }

    public void softDelete() {
        if (deletedAt == null) {
            isActive = false;
            deletedAt = Instant.now();
        }
    }

    //เรียงลำดับของ Task ใน Project โดยใช้ sortOrder
    //ถ้า sortOrder เป็นลบจะเกิด IllegalArgumentException
    public void reorder(int position) {
        if (position < 0) {
            throw new IllegalArgumentException(
                    "sortOrder must not be negative"
            );
        }
        sortOrder = position;
    }


    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "name is required"
            );
        }
        return name.trim();
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

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public Instant getDeletedAt() {
        return deletedAt;
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
