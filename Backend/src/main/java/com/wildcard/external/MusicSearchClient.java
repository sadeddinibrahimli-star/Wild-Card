package com.wildcard.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Musiqi axtarışı iTunes Search API (açar tələb etmir).
 * TheAudioDB istifadə OLUNMUR — premium açar tələb edir.
 */
@Slf4j
@Component
public class MusicSearchClient {

    private static final String ITUNES_URL = "https://itunes.apple.com/search";
    private static final int LIMIT = 8;

    private final ExternalHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public MusicSearchClient(ExternalHttpClient http) {
        this.http = http;
    }

    public List<MusicHit> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String url = ITUNES_URL + "?term=" + ExternalHttpClient.encode(query.trim())
                + "&entity=song&limit=" + LIMIT;

        String json = http.getJson(url);
        if (json.isBlank()) {
            return List.of();
        }

        List<MusicHit> out = new ArrayList<>();
        try {
            JsonNode results = mapper.readTree(json).path("results");
            if (!results.isArray()) {
                return List.of();
            }
            for (JsonNode m : results) {
                if (out.size() >= LIMIT) {
                    break;
                }
                // yalnız mahnı (collection) növünü götürürük
                if (!"track".equals(m.path("wrapperType").asText(null))) {
                    continue;
                }
                String track = emptyToNull(m.path("trackName").asText(null));
                if (track == null) {
                    continue;
                }
                out.add(new MusicHit(
                        emptyToNull(m.path("artistName").asText(null)),
                        track,
                        enlarge(emptyToNull(m.path("artworkUrl100").asText(null)))));
            }
        } catch (Exception e) {
            log.warn("iTunes cavabı oxunmadı: {}", e.getMessage());
            return List.of();
        }
        return out;
    }

    /** artworkUrl100 -> 300x300 (100x100 sonu iki dəfə dəyişir). */
    private String enlarge(String artwork) {
        if (artwork == null || !artwork.contains("100x100")) {
            return artwork;
        }
        return artwork.replace("100x100", "300x300");
    }

    private String emptyToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    public record MusicHit(String artist, String track, String albumArtUrl) {
    }
}