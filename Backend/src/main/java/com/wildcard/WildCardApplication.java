package com.wildcard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class WildCardApplication {

    /**
     * Profiles: docker and prod are production.
     * In these profiles the secrets are MANDATORY and the application does not
     * start without them. dev/test skip the check (local development may use
     * default values).
     */
    private static final List<String> STRICT_PROFILES = List.of("docker", "prod");

    public static void main(String[] args) {
        requireSecretsIfProduction();
        SpringApplication.run(WildCardApplication.class, args);
    }

    /**
     * Stops the application BEFORE it starts.
     *
     * Why inside main()?
     *  - @PostConstruct + @ConfigurationProperties only run after the connection
     *    is established, and the error then looks like a "connection failed";
     *  - Spring resolves ${VAR} with ignoreUnresolvablePlaceholders=true, so a
     *    missing variable in yml never throws on its own.
     *  main() runs before Spring starts: no beans, no TCP connection, nothing
     *  "remote".
     *
     * The secrets themselves are never printed - only what is wrong.
     */
    static void requireSecretsIfProduction() {
        String[] active = System.getProperty("spring.profiles.active",
                System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "")).split("[,\\s]+");

        boolean strict = Arrays.stream(active)
                .map(String::trim)
                .anyMatch(STRICT_PROFILES::contains);

        if (!strict) {
            return;
        }

        List<String> problems = new ArrayList<>();

        if (blank(System.getenv("DB_PASSWORD"))) {
            problems.add("DB_PASSWORD is required");
        }
        String jwt = System.getenv("JWT_SECRET");
        if (blank(jwt)) {
            problems.add("JWT_SECRET is required (32+ characters)");
        } else if (jwt.length() < 32) {
            problems.add("JWT_SECRET must be at least 32 characters (got " + jwt.length() + ")");
        }
        if (Boolean.parseBoolean(env("ADMIN_SEED_ENABLED", "false"))) {
            if (blank(System.getenv("ADMIN_EMAIL"))) {
                problems.add("ADMIN_EMAIL is required when ADMIN_SEED_ENABLED=true");
            }
            String adminPassword = System.getenv("ADMIN_PASSWORD");
            if (blank(adminPassword)) {
                problems.add("ADMIN_PASSWORD is required when ADMIN_SEED_ENABLED=true");
            } else if (adminPassword.length() < 8) {
                problems.add("ADMIN_PASSWORD must be at least 8 characters (got "
                        + adminPassword.length() + ")");
            }
        }

        if (!problems.isEmpty()) {
            System.err.println();
            System.err.println("  WildCard refuses to start in a production profile.");
            System.err.println("  Required secrets are missing or too weak:");
            for (String problem : problems) {
                System.err.println("    - " + problem);
            }
            System.err.println("  See .env.example for the full list.");
            System.err.println();
            throw new IllegalStateException("Missing or weak secrets: " + String.join("; ", problems));
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}