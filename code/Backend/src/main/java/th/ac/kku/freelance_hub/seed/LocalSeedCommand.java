package th.ac.kku.freelance_hub.seed;

import java.net.URI;
import java.util.Set;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import th.ac.kku.freelance_hub.FreelanceHubApplication;

/**
 * One-shot local development seeder. Not part of normal application startup.
 */
public final class LocalSeedCommand {

    private static final Set<String> LOCAL_DATABASE_HOSTS = Set.of("localhost", "127.0.0.1", "postgres");

    private LocalSeedCommand() {
    }

    public static void main(String[] args) {

        if (!"true".equalsIgnoreCase(System.getenv("LOCAL_SEED_RUN"))) {
            throw new IllegalStateException("Set LOCAL_SEED_RUN=true explicitly to use the local seeder.");
        }
        if (System.getenv("RENDER") != null || System.getenv("CI") != null) {
            throw new IllegalStateException("Local seed is disabled in CI and Render environments.");
        }
        String password = System.getenv("LOCAL_SEED_PASSWORD");
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Set LOCAL_SEED_PASSWORD (at least 8 characters) before seeding.");
        }

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(FreelanceHubApplication.class)
                .profiles("local-seed")
                .web(WebApplicationType.NONE)
                .initializers(applicationContext -> requireLocalDatabase(
                        applicationContext.getEnvironment().getProperty("spring.datasource.url")))
                .run(args)) {
            String email = System.getenv().getOrDefault("LOCAL_SEED_EMAIL", "seed.local@example.test");
            LocalSeedService.SeedResult result = context.getBean(LocalSeedService.class).seed(email, password);
            System.out.printf("Local seed ready: user=%s client=%s project=%s task=%s%n",
                    result.userId(), result.clientId(), result.projectId(), result.taskId());
        }
    }

    private static void requireLocalDatabase(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("Local seed requires an explicit PostgreSQL JDBC URL.");
        }
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        if (!LOCAL_DATABASE_HOSTS.contains(uri.getHost()) || uri.getUserInfo() != null) {
            throw new IllegalStateException(
                    "Local seed may connect only to localhost or the local Docker postgres service.");
        }
    }
}
