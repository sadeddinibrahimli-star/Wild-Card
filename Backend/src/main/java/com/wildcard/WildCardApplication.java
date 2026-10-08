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
     * Profil: docker və prod = istehsal.
     * Bu profillərdə sirlar MƏCBURİDİR və yoxlanmasa tətbiq açılmır.
     * dev/test-də yoxlama işləmir (lokal inkişaf üçün default dəyərlər icazəlidir).
     */
    private static final List<String> STRICT_PROFILES = List.of("docker", "prod");

    public static void main(String[] args) {
        requireSecretsIfProduction();
        SpringApplication.run(WildCardApplication.class, args);
    }

    /**
     * Tətbiq AÇILMADAN ƏVVƏL dayanır.
     *
     * Niyə main() daxilində?
     *  - @PostConstruct və @ConfigurationProperties bağlantı qurulduqdan
     *    SONRA yoxlayır və xəta "connection failed" kimi görünür;
     *  - Spring `ignoreUnresolvablePlaceholders=true` ilə işlədiyi üçün
     *    yml-də ${VAR} yazsan belə xəta atmır.
     *  main() isə SPIRAL-dan da əvvəl işləyir: heç bir bean yaranmır,
     *  heç bir TCP bağlantısı açılmır, heç nə "uzaq" deyil.
     *
     * Sirların ÖZÜ heç vaxt mesaja yazılmır - yalnız problemlər yazılır.
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

        // 1) DB parolu
        if (blank(System.getenv("DB_PASSWORD"))) {
            problems.add("DB_PASSWORD is required");
        }
        // 2) JWT açarı - 32+ simvol
        String jwt = System.getenv("JWT_SECRET");
        if (blank(jwt)) {
            problems.add("JWT_SECRET is required (32+ characters)");
        } else if (jwt.length() < 32) {
            problems.add("JWT_SECRET must be at least 32 characters (got " + jwt.length() + ")");
        }
        // 3) Admin seed parolu - yalnız seed açıqdursa
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