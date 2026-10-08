package com.wildcard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Every environment variable is read HERE. No key is hardcoded as a
 * default in the code - otherwise the feature silently degrades
 * (and is logged), but the app never crashes.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "wildcard")
public class WildcardProperties {

    private final Jwt jwt = new Jwt();
    private final Admin admin = new Admin();
    private final RateLimit rateLimit = new RateLimit();
    private final Affinity affinity = new Affinity();
    private final Storage storage = new Storage();

    @Getter
    @Setter
    public static class Jwt {
        /** When empty, JwtService generates a temporary development key. */
        private String secret = "";
        private long accessTokenExpirationMinutes = 60;
        private long refreshTokenExpirationDays = 30;
        private String issuer = "wild-card";
    }

    @Getter
    @Setter
    public static class Admin {
        private boolean enabled = false;
        private String email = "";
        private String password = "";
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int postsPerWindow = 5;
        private int commentsPerWindow = 10;
        private int watchlistPerWindow = 10;
        private long windowSeconds = 60;
    }

    @Getter
    @Setter
    public static class Affinity {
        private double reactionWeight = 1.0;
        private double threshold = 5.0;
        private double explicitScore = 10.0;
    }

    @Getter
    @Setter
    public static class Storage {
        private String localDir = "uploads";
    }

    private final Seed seed = new Seed();

    @Getter
    @Setter
    public static class Seed {
        private boolean enabled = false;
        /** Password of the demo users - never hardcoded. */
        private String userPassword = "";
    }

    /** Base URL of the frontend - the password reset link is built from it. */
    private String frontendUrl = "http://localhost:5173";

    /** Production guard: when enabled the app refuses to start with an empty key. */
    private boolean requireSecrets = false;

}
