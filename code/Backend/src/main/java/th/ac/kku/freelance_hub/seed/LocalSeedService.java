package th.ac.kku.freelance_hub.seed;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.domain.valueobject.Address;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.security.EmailNormalizer;

/** Creates a stable, owner-owned data set for local UI testing. */
@Service
@Profile("local-seed")
@RequiredArgsConstructor
public class LocalSeedService {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final String LEGACY_CLIENT = "Example Client [local-seed]";
    private static final String LEGACY_PROJECT = "Example Project [local-seed]";
    private static final String LEGACY_TASK = "Example Task [local-seed]";

    private static final List<ClientSpec> CLIENTS = List.of(
            new ClientSpec("client-01", "Mekong Creative Studio", "Mekong Creative Co., Ltd.",
                    "contact@mekong.example.test", "0801000001", "123/1 ถนนมิตรภาพ", "ในเมือง", "เมืองขอนแก่น",
                    "ขอนแก่น", "40000", "0105559000001", "Primary client for local UI testing."),
            new ClientSpec("client-02", "Northstar Learning", "Northstar Education", "hello@northstar.example.test",
                    "0801000002", "88 ถนนกสิกรทุ่งสร้าง", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000",
                    "0105559000002", "Education platform client."),
            new ClientSpec("client-03", "Siam Green Market", "Siam Green Retail", "team@siamgreen.example.test",
                    "0801000003", "29 ถนนศรีจันทร์", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000", "0105559000003",
                    "Retail client with complete contact details."),
            new ClientSpec("client-04", "Bluebird Health", "Bluebird Health Services", "admin@bluebird.example.test",
                    "0801000004", "45/7 ถนนประชาสโมสร", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000", "0105559000004",
                    "Healthcare service client."),
            new ClientSpec("client-05", "Riverstone Cafe", "Riverstone Hospitality", "owner@riverstone.example.test",
                    "0801000005", "77 ถนนกลางเมือง", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000", "0105559000005",
                    "Hospitality client."),
            new ClientSpec("client-06", "Orbit Logistics", "Orbit Logistics Thailand", "ops@orbit.example.test",
                    "0801000006", "101 ถนนมิตรภาพ", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000", "0105559000006",
                    "Logistics client."),
            new ClientSpec("client-07", "Lantern Books", "Lantern Publishing", "editor@lantern.example.test",
                    "0801000007", "19 ถนนหน้าเมือง", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000", "0105559000007",
                    "Publishing client."),
            new ClientSpec("client-08", "Cloudline Travel", "Cloudline Travel Group", "booking@cloudline.example.test",
                    "0801000008", "52 ถนนกัลปพฤกษ์", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000", "0105559000008",
                    "Travel client."),
            new ClientSpec("client-09", "Juniper Home", "Juniper Home Living", "support@juniper.example.test",
                    "0801000009", "9/9 ถนนมะลิวัลย์", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000", "0105559000009",
                    "Home goods client."),
            new ClientSpec("client-10", "Pixel Orchard", "", "", "", "", "", "", "", "", "",
                    "Sparse client record for empty contact fields."),
            new ClientSpec("client-11", "Solo Consultant", "", "solo@example.test", "", "", "", "", "", "", "",
                    "Sparse client record with no address."),
            new ClientSpec("client-12", "Archived Sample Client", "Archived Sample Co.", "archive@example.test",
                    "0801000012", "12 ถนนมิตรภาพ", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000", "0105559000012",
                    "Archived client for status filter testing."));

    private static final List<ProjectSpec> PROJECTS = List.of(
            new ProjectSpec("project-01", "Website Redesign", "client-01", ProjectStatus.ACTIVE,
                    "Responsive marketing site redesign and implementation.", "#2563EB", 600, 0),
            new ProjectSpec("project-02", "Learning Portal MVP", "client-02", ProjectStatus.ACTIVE,
                    "Core learning portal flows and accessible UI.", "#7C3AED", 900, 2),
            new ProjectSpec("project-03", "Green Market Storefront", "client-03", ProjectStatus.ACTIVE,
                    "Online storefront and product catalog.", "#16A34A", 720, 5),
            new ProjectSpec("project-04", "Bluebird Booking Flow", "client-04", ProjectStatus.ACTIVE,
                    "Appointment booking experience.", "#0891B2", 480, 7),
            new ProjectSpec("project-05", "Cafe Loyalty App", "client-05", ProjectStatus.ACTIVE,
                    "Mobile-first loyalty program.", "#EA580C", 540, 10),
            new ProjectSpec("project-06", "Orbit Operations Dashboard", "client-06", ProjectStatus.ACTIVE,
                    "Operations dashboard and data views.", "#4F46E5", 800, 15),
            new ProjectSpec("project-07", "Lantern Catalog", "client-07", ProjectStatus.ACTIVE,
                    "Searchable publishing catalog.", "#BE123C", 360, 20),
            new ProjectSpec("project-08", "Cloudline Campaign", "client-08", ProjectStatus.PLANNED,
                    "Campaign landing page planned for next quarter.", "#0F766E", 300, 0),
            new ProjectSpec("project-09", "Juniper Product Pages", "client-09", ProjectStatus.PLANNED,
                    "Product pages awaiting kickoff.", "#A16207", 420, 0),
            new ProjectSpec("project-10", "Pixel Orchard Brand Kit", "client-10", ProjectStatus.ON_HOLD,
                    "Brand kit paused pending stakeholder review.", "#9333EA", 240, 5),
            new ProjectSpec("project-11", "Solo Portfolio Build", "client-11", ProjectStatus.COMPLETED,
                    "Completed portfolio build for a solo consultant.", "#475569", 300, 45),
            new ProjectSpec("project-12", "Archived Legacy Site", "client-01", ProjectStatus.ARCHIVED,
                    "Archived project kept for status filter testing.", "#64748B", 180, 60));

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Transactional
    public SeedResult seed(String rawEmail, String password, boolean runningTimerRequested) {
        String email = EmailNormalizer.normalize(rawEmail);
        if (email == null || email.isBlank() || password == null || password.length() < 8) {
            throw new IllegalArgumentException("A valid email and password of at least 8 characters are required.");
        }

        ensureMappingTable();
        Counts counts = new Counts();
        var existingUser = userRepository.findByEmail(email);
        User user = existingUser.orElseGet(() -> createUser(email, password));
        counts.add("user", existingUser.isPresent());
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalStateException(
                    "Seed user already exists with a different password; choose another LOCAL_SEED_EMAIL.");
        }
        if (user.getProfile() == null) {
            UserProfile profile = createProfile();
            user.setProfile(profile);
            counts.created.merge("profile", 1, Integer::sum);
        } else if (existingUser.isEmpty()) {
            counts.created.merge("profile", 1, Integer::sum);
        } else {
            counts.existing.merge("profile", 1, Integer::sum);
        }

        Map<String, Client> clients = new LinkedHashMap<>();
        for (ClientSpec spec : CLIENTS) {
            Client client = getOrCreate("client", spec.key(), user.getId(), Client.class,
                    () -> findLegacyClient(user, spec), () -> createClient(user, spec), counts);
            clients.put(spec.key(), client);
        }
        entityManager.flush();

        Map<String, Project> projects = new LinkedHashMap<>();
        for (ProjectSpec spec : PROJECTS) {
            Project project = getOrCreate("project", spec.key(), user.getId(), Project.class,
                    () -> findLegacyProject(user, clients.get(spec.clientKey()), spec),
                    () -> createProject(user, clients.get(spec.clientKey()), spec), counts);
            projects.put(spec.key(), project);
        }
        entityManager.flush();

        Map<String, Task> tasks = seedTasks(user, projects, counts);
        seedCompletedEntries(user, projects, tasks, counts);
        boolean timerRunningOrCreated = seedOptionalRunningTimer(user, projects.get("project-01"), tasks.get("task-01"),
                runningTimerRequested, counts);

        entityManager.flush();
        printSummary(email, counts, timerRunningOrCreated);
        return new SeedResult(user.getId(), Map.copyOf(counts.created), Map.copyOf(counts.existing),
                timerRunningOrCreated);
    }

    private User createUser(String email, String password) {
        User user = User.builder().email(email).passwordHash(passwordEncoder.encode(password)).build();
        user.setProfile(createProfile());
        return userRepository.save(user);
    }

    private UserProfile createProfile() {
        return UserProfile.builder()
                .displayName("Local Seed User").firstName("Demo").lastName("Freelancer")
                .phone("0800000001")
                .addressDetails(new Address("123/1 ถนนมิตรภาพ", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000"))
                .taxId("0000000000000")
                .bio("Freelance web developer and designer. Fictional profile for local testing.")
                .build();
    }

    private Client createClient(User user, ClientSpec spec) {
        Client client = new Client(user, displayName(spec.name()));
        client.updateDetailsWithAddress(displayName(spec.name()), blankToNull(spec.company()),
                blankToNull(spec.email()),
                blankToNull(spec.phone()), blankToNull(spec.address()), blankToNull(spec.subdistrict()),
                blankToNull(spec.district()), blankToNull(spec.province()), blankToNull(spec.postalCode()),
                blankToNull(spec.taxId()), spec.notes());
        if (spec.key().equals("client-12"))
            client.setActive(false);
        entityManager.persist(client);
        return client;
    }

    private Client findLegacyClient(User user, ClientSpec spec) {
        if (!spec.key().equals("client-01"))
            return null;
        return entityManager
                .createQuery("select c from Client c where c.owner.id = :owner and c.name in :names", Client.class)
                .setParameter("owner", user.getId())
                .setParameter("names", List.of(LEGACY_CLIENT, "[local-seed] Example Client"))
                .setMaxResults(1)
                .getResultStream().findFirst().orElse(null);
    }

    private Project createProject(User user, Client client, ProjectSpec spec) {
        LocalDate today = LocalDate.now(clock.withZone(BANGKOK));
        Project project = new Project(user, client, displayName(spec.name()));
        LocalDate endDate = spec.status() == ProjectStatus.COMPLETED ? today.minusDays(3)
                : today.plusDays(30 + spec.startDaysAgo());
        project.updateDetails(displayName(spec.name()), spec.description(), today.minusDays(spec.startDaysAgo()),
                endDate, spec.color(), spec.targetMinutes());
        if (spec.status() == ProjectStatus.ON_HOLD || spec.status() == ProjectStatus.COMPLETED) {
            project.changeStatus(ProjectStatus.ACTIVE);
        }
        project.changeStatus(spec.status());
        entityManager.persist(project);
        return project;
    }

    private Project findLegacyProject(User user, Client client, ProjectSpec spec) {
        if (!spec.key().equals("project-01"))
            return null;
        return entityManager.createQuery(
                "select p from Project p where p.owner.id = :owner and p.client.id = :client and p.name in :names",
                Project.class)
                .setParameter("owner", user.getId()).setParameter("client", client.getId())
                .setParameter("names", List.of(LEGACY_PROJECT, "[local-seed] Example Project"))
                .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private Map<String, Task> seedTasks(User user, Map<String, Project> projects, Counts counts) {
        Map<String, Task> tasks = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            final int taskNumber = i;
            String key = String.format("task-%02d", i);
            TaskStatus status = switch ((i - 1) % 3) {
                case 0 -> TaskStatus.OPEN;
                case 1 -> TaskStatus.IN_PROGRESS;
                default -> TaskStatus.COMPLETED;
            };
            Task task = getOrCreate("task", key, user.getId(), Task.class,
                    () -> taskNumber == 1 ? findLegacyTask(projects.get("project-01")) : null,
                    () -> createTask(projects.get("project-01"), taskNumber - 1, "Website task " + taskNumber,
                            "Homepage delivery task " + taskNumber + " with design, implementation and review details.",
                            status),
                    counts);
            tasks.put(key, task);
        }
        String[][] extras = {
                { "task-13", "project-02", "Prepare course dashboard", "Add course progress and learner summary.",
                        "OPEN" },
                { "task-14", "project-03", "Build product filter",
                        "Filter storefront products by category and availability.", "IN_PROGRESS" },
                { "task-15", "project-04", "Confirm booking flow",
                        "Completed acceptance checks for appointment booking.", "COMPLETED" },
                { "task-16", "project-11", "Publish portfolio", "Final portfolio content and production review.",
                        "COMPLETED" }
        };
        for (int i = 0; i < extras.length; i++) {
            String[] row = extras[i];
            String key = row[0];
            TaskStatus status = TaskStatus.valueOf(row[4]);
            Task task = getOrCreate("task", key, user.getId(), Task.class, () -> null,
                    () -> createTask(projects.get(row[1]), 0, row[2], row[3], status), counts);
            tasks.put(key, task);
        }
        entityManager.flush();
        return tasks;
    }

    private Task createTask(Project project, int sortOrder, String name, String description, TaskStatus status) {
        Task task = new Task(project, name, sortOrder);
        task.updateDetails(name, description);
        if (status == TaskStatus.IN_PROGRESS || status == TaskStatus.COMPLETED)
            task.start();
        if (status == TaskStatus.COMPLETED)
            task.complete(Instant.now(clock).minusSeconds(3600));
        entityManager.persist(task);
        return task;
    }

    private Task findLegacyTask(Project project) {
        return entityManager
                .createQuery("select t from Task t where t.project.id = :project and t.name in :names", Task.class)
                .setParameter("project", project.getId())
                .setParameter("names", List.of(LEGACY_TASK, "[local-seed] Example Task"))
                .setMaxResults(1)
                .getResultStream().findFirst().orElse(null);
    }

    private void seedCompletedEntries(User user, Map<String, Project> projects, Map<String, Task> tasks,
            Counts counts) {
        LocalDate today = LocalDate.now(clock.withZone(BANGKOK));
        for (int i = 1; i <= 14; i++) {
            final int entryNumber = i;
            String key = String.format("entry-%02d", i);
            getOrCreate("entry", key, user.getId(), TimeEntry.class, () -> null,
                    () -> createEntry(user, projects, tasks, today, entryNumber), counts);
        }
        entityManager.flush();
    }

    private TimeEntry createEntry(User user, Map<String, Project> projects, Map<String, Task> tasks, LocalDate today,
            int number) {
        String projectKey = switch (number) {
            case 1, 2, 3, 4, 5, 6, 7, 8, 9 -> "project-01";
            case 10, 11 -> "project-02";
            case 12 -> "project-03";
            case 13 -> "project-04";
            default -> "project-05";
        };
        String taskKey = number == 1 || number == 4 || number == 7 ? "task-01"
                : number == 2 || number == 5 || number == 8 ? "task-02"
                        : number == 10 ? "task-13" : number == 12 ? "task-14" : number == 13 ? "task-15" : null;
        LocalDate date = switch (number) {
            case 1 -> today;
            case 2 -> today.minusDays(1);
            case 3, 4, 5 -> today.minusDays(7 + number);
            default -> today.minusDays(14 + number);
        };
        long durationSeconds = (25L + (number % 5) * 20L) * 60;
        LocalTime startTime = number == 1
                ? LocalTime.now(clock.withZone(BANGKOK)).minusHours(4).withSecond(0).withNano(0)
                : LocalTime.of(9 + (number % 6), 0);
        Instant start = date.atTime(startTime).atZone(BANGKOK).toInstant();
        Project project = projects.get(projectKey);
        Task task = taskKey == null ? null : tasks.get(taskKey);
        String description = "Local seed work session " + number + " for " + project.getName() + ".";
        TimeEntry entry;
        if (number <= 12) {
            entry = TimeEntry.createManualWithDurationSeconds(user, project, task, description, start, durationSeconds);
        } else {
            entry = TimeEntry.startTimer(user, project, task, description, start);
            entry.stop(start.plusSeconds(durationSeconds));
        }
        entityManager.persist(entry);
        return entry;
    }

    private boolean seedOptionalRunningTimer(User user, Project project, Task task, boolean requested, Counts counts) {
        if (!requested)
            return false;
        if (timeEntryRepository.findByOwnerIdAndEntryTypeAndEndedAtIsNullAndIsActiveTrue(user.getId(), EntryType.TIMER)
                .isPresent()) {
            counts.runningTimer = "existing timer preserved";
            return true;
        }
        TimeEntry timer = TimeEntry.startTimer(user, project, task, "Optional local seed running timer",
                Instant.now(clock).minusSeconds(300));
        entityManager.persist(timer);
        counts.created.merge("running timer", 1, Integer::sum);
        counts.runningTimer = "created";
        return true;
    }

    private <T> T getOrCreate(String kind, String key, UUID ownerId, Class<T> type,
            EntitySupplier<T> legacyLookup, EntitySupplier<T> create, Counts counts) {
        UUID id = findMapping(ownerId, kind, key);
        T entity = id == null ? null : loadOwned(type, id, ownerId);
        if (entity == null && id == null)
            entity = legacyLookup.get();
        if (entity == null) {
            entity = create.get();
            counts.created.merge(kind, 1, Integer::sum);
        } else {
            counts.existing.merge(kind, 1, Integer::sum);
        }
        writeMapping(ownerId, kind, key, entityId(entity));
        return entity;
    }

    private <T> T loadOwned(Class<T> type, UUID id, UUID ownerId) {
        Object entity = entityManager.find(type, id);
        if (entity == null)
            return null;
        UUID entityOwnerId;
        if (entity instanceof Client value)
            entityOwnerId = value.getOwner().getId();
        else if (entity instanceof Project value)
            entityOwnerId = value.getOwner().getId();
        else if (entity instanceof Task value)
            entityOwnerId = value.getProject().getOwner().getId();
        else if (entity instanceof TimeEntry value)
            entityOwnerId = value.getOwner().getId();
        else
            throw new IllegalArgumentException("Unsupported local seed entity type: " + type);
        return ownerId.equals(entityOwnerId) ? type.cast(entity) : null;
    }

    private UUID findMapping(UUID ownerId, String kind, String key) {
        List<UUID> rows = jdbcTemplate.query(
                "select entity_id from local_seed_records where owner_id = ? and kind = ? and seed_key = ?",
                (rs, row) -> rs.getObject(1, UUID.class), ownerId, kind, key);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void writeMapping(UUID ownerId, String kind, String key, UUID entityId) {
        int updated = jdbcTemplate.update(
                "update local_seed_records set entity_id = ? where owner_id = ? and kind = ? and seed_key = ?",
                entityId, ownerId, kind, key);
        if (updated == 0)
            jdbcTemplate.update(
                    "insert into local_seed_records (owner_id, kind, seed_key, entity_id) values (?, ?, ?, ?)",
                    ownerId, kind, key, entityId);
    }

    private void ensureMappingTable() {
        jdbcTemplate.execute(
                "create table if not exists local_seed_records (owner_id uuid not null, kind varchar(24) not null, seed_key varchar(100) not null, entity_id uuid not null, primary key (owner_id, kind, seed_key))");
    }

    private static UUID entityId(Object entity) {
        if (entity instanceof Client value)
            return value.getId();
        if (entity instanceof Project value)
            return value.getId();
        if (entity instanceof Task value)
            return value.getId();
        if (entity instanceof TimeEntry value)
            return value.getId();
        throw new IllegalArgumentException("Unsupported local seed entity: " + entity.getClass());
    }

    private static String displayName(String name) {
        return name.endsWith(" [seed]") ? name : name + " [seed]";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void printSummary(String email, Counts counts, boolean timer) {
        System.out.println("Local seed ready for " + email + " (password not displayed)");
        for (String kind : List.of("user", "profile", "client", "project", "task", "entry", "running timer")) {
            System.out.printf("  %-13s created=%d existing=%d%n", kind,
                    counts.created.getOrDefault(kind, 0), counts.existing.getOrDefault(kind, 0));
        }
        System.out.println("  running timer: " + (timer ? counts.runningTimer : "not requested"));
    }

    @FunctionalInterface
    private interface EntitySupplier<T> {
        T get();
    }

    private static final class Counts {
        private final Map<String, Integer> created = new LinkedHashMap<>();
        private final Map<String, Integer> existing = new LinkedHashMap<>();
        private String runningTimer = "not requested";

        private void add(String kind, boolean alreadyExists) {
            (alreadyExists ? existing : created).merge(kind, 1, Integer::sum);
        }
    }

    private record ClientSpec(String key, String name, String company, String email, String phone,
            String address, String subdistrict, String district, String province, String postalCode,
            String taxId, String notes) {
    }

    private record ProjectSpec(String key, String name, String clientKey, ProjectStatus status,
            String description, String color, int targetMinutes, int startDaysAgo) {
    }

    public record SeedResult(UUID userId, Map<String, Integer> created, Map<String, Integer> existing,
            boolean runningTimer) {
    }
}
