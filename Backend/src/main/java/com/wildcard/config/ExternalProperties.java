package com.wildcard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Keys for external services and mail settings.
 *
 * No key is hardcoded. When a key is empty the related feature simply
 * degrades (search disabled / link printed to the console) - the app does
 * not crash.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "wildcard.external")
public class ExternalProperties {

    private final Tmdb tmdb = new Tmdb();
    private final Mail mail = new Mail();
    private final Http http = new Http();

    @Getter
    @Setter
    public static class Tmdb {
        /** When empty, FILM search is disabled. */
        private String apiKey = "";
        private String baseUrl = "https://api.themoviedb.org/3";
        private String imageBase = "https://image.tmdb.org/t/p/w185";
    }

    @Getter
    @Setter
    public static class Mail {
        private String host = "";
        private Integer port = 25;
        private String username = "";
        private String password = "";
        private String from = "";

        public int portOrDefault() {
            return port == null ? 25 : port;
        }

        public boolean isConfigured() {
            return host != null && !host.isBlank();
        }
    }

    @Getter
    @Setter
    public static class Http {
        private long timeoutSeconds = 5;
        private long cacheMinutes = 10;
    }
}