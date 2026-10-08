package com.wildcard.external;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wildcard.config.ExternalProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Xarici API-yə gedən bütün çağırışlar buradan keçir.
 *
 * Qaydalar:
 *  - timeout 5 saniyə
 *  - cavab 10 dəqiqə cache-lənir
 *  - xəta olanda boş siyahı qaytarılır və WARN yazılır (502 yoxdur)
 */
@Slf4j
@Component
public class ExternalHttpClient {

    private final ExternalProperties props;
    private final HttpClient http;
    private final Cache<String, String> cache;

    public ExternalHttpClient(ExternalProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(props.getHttp().getTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(props.getHttp().getCacheMinutes()))
                .maximumSize(2_000)
                .build();
    }

    /** GET — boş String qaytarırsa xəta baş verib və ya cache boşdur. */
    public String getJson(String url) {
        String cached = cache.getIfPresent(url);
        if (cached != null) {
            return cached;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(props.getHttp().getTimeoutSeconds()))
                    .header("Accept", "application/json")
                    .header("User-Agent", "WildCard/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("External API {} returned {}", host(url), response.statusCode());
                return "";
            }
            cache.put(url, response.body());
            return response.body();
        } catch (Exception e) {
            // xəta olanda tətbiq ÇÖKMƏZ, boş siyahı qaytarılır
            log.warn("External API {} failed: {}", host(url), e.getMessage());
            return "";
        }
    }

    /** POST JSON (AniList GraphQL). */
    public String postJson(String url, String body) {
        String key = "POST " + url + " " + body.hashCode();
        String cached = cache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(props.getHttp().getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("External API {} returned {}", host(url), response.statusCode());
                return "";
            }
            cache.put(key, response.body());
            return response.body();
        } catch (Exception e) {
            log.warn("External API {} failed: {}", host(url), e.getMessage());
            return "";
        }
    }

    /** Açarı URL-ə yazmır (log/mesaj təhlükəsi). */
    private String host(String url) {
        int i = url.indexOf("//");
        if (i < 0) {
            return "external";
        }
        int end = url.indexOf('/', i + 2);
        return end < 0 ? url.substring(i + 2) : url.substring(i + 2, end);
    }

    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}