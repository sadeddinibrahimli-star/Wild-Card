package com.wildcard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bütün mühit dəyişənləri BURADA oxunur. Heç bir açar kodda sabit
 * (default) dəyər olaraq yazılmır — əks halda funksiya sadələşir və
 * loglanır, tətbiq ÇÖKMƏMİR.
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
        /** Boşdursa JwtService development üçün müvəqqəti açar yaradır. */
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

    /** Lokal demo məlumatı. İstehsalda false. */
    private final Seed seed = new Seed();

    @Getter
    @Setter
    public static class Seed {
        private boolean enabled = false;
        /** Demo istifadəçilərinin parolu - heç vaxt koda yazılmır. */
        private String userPassword = "";
    }

    /** Frontend-in ümumi URL-i - parol sıfırlama linki bundan yığılır. */
    private String frontendUrl = "http://localhost:5173";

    /** İstehsalda dayanmaq üçün: açar boşdursa tətbiq açılmır. */
    private boolean requireSecrets = false;

}
