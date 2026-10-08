package com.wildcard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Xarici xidmətlərin açarları və e-poçt ayarları.
 *
 * Heç bir açar sabit yazılmır. Açar boşdursa müvafiq funksiya
 * sadələşir (axtarış söndürülür / link konsola yazılır) — tətbiq ÇÖKMƏZ.
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
        /** Boşdursa FILM axtarışı söndürülür. */
        private String apiKey = "";
        private String baseUrl = "https://api.themoviedb.org/3";
        private String imageBase = "https://image.tmdb.org/t/p/w185";
    }

    @Getter
    @Setter
    public static class Mail {
        private String host = "";
        /** Boş dəyər (env-da MAIL_PORT=) 25-ə düşür. */
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