package com.wildcard.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildcard.config.ExternalProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Anime axtarışı AniList GraphQL, film axtarışı TMDB.
 *
 * AniList açar tələb etmir. TMDB açarı boşdursa FILM axtarışı
 * söndürülür və boş siyahı qaytarılır (tətbiq çökmür).
 *
 * Heç bir açar loga və ya cavaba yazılmır.
 */
@Slf4j
@Component
public class TitleSearchClient {

    private static final String ANILIST_URL = "https://graphql.anilist.co";
    private static final int ANILIST_PER_MINUTE = 90;
    private static final int LIMIT = 8;

    private static final String ANIME_QUERY = """
            query ($s: String) {
              Page(perPage: %d) {
                media(search: $s, type: __TYPE__) {
                  id
                  title { romaji english }
                  seasonYear
                  coverImage { medium }
                  averageScore
                  episodes
                }
              }
}
            """.formatted(LIMIT);
    private final ExternalHttpClient http;
    private final ExternalProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    /** AniList dəqiqədə 90 request limiti. */
    private final AniListGuard aniListGuard = new AniListGuard(ANILIST_PER_MINUTE);

    public TitleSearchClient(ExternalHttpClient http, ExternalProperties props) {
        this.http = http;
        this.props = props;
    }

    public boolean filmSearchEnabled() {
        String key = props.getTmdb().getApiKey();
        return key != null && !key.isBlank();
    }

    /** type: ANIME | FILM */
    public List<SearchHit> search(String query, String type) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return "FILM".equalsIgnoreCase(type) ? searchFilms(query) : searchAnime(query);
    }

    // ---------------- ANIME (AniList) ----------------

    private List<SearchHit> searchAnime(String query) {
        return searchAniList(query, "ANIME");
    }

    /** AniList: ANIME və ya MOVIE. İkisi də açar tələb etmir. */
    private List<SearchHit> searchAniList(String query, String type) {
        if (!aniListGuard.allow()) {
            log.warn("AniList rate limit reached ({} per minute)", ANILIST_PER_MINUTE);
            return List.of();
        }

        String anilistQuery = ANIME_QUERY.replace("__TYPE__", type);
        String body = mapper.createObjectNode()
                .put("query", anilistQuery)
                .set("variables", mapper.createObjectNode().put("s", query))
                .toString();

        String json = http.postJson(ANILIST_URL, body);
        if (json.isBlank()) {
            return List.of();
        }

        List<SearchHit> out = new ArrayList<>();
        try {
            JsonNode media = mapper.readTree(json)
                    .path("data").path("Page").path("media");
            if (!media.isArray()) {
                return List.of();
            }
            for (JsonNode m : media) {
                if (out.size() >= LIMIT) {
                    break;
                }
                String title = firstNonBlank(
                        m.path("title").path("romaji").asText(null),
                        m.path("title").path("english").asText(null));
                if (title == null) {
                    continue;
                }
                out.add(SearchHit.builder()
                        .externalId(m.path("id").isMissingNode() ? null : String.valueOf(m.path("id").asLong()))
                        .title(title)
                        .year(m.path("seasonYear").isNull() ? null : String.valueOf(m.path("seasonYear").asInt()))
                        .posterUrl(emptyToNull(m.path("coverImage").path("medium").asText(null)))
                        .score(normalizeScore(m.path("averageScore").asDouble()))
                        .extra(m.path("episodes").isNull() ? null : String.valueOf(m.path("episodes").asInt()))
                        .build());
            }
        } catch (Exception e) {
            log.warn("AniList cavabı oxunmadı: {}", e.getMessage());
            return List.of();
        }
        return out;
    }

    // ---------------- FILM (TMDB) ----------------

    private List<SearchHit> searchFilms(String query) {
        String key = props.getTmdb().getApiKey();
        if (key == null || key.isBlank()) {
            // TMDB acari yoxdur - AniList MOVIE ile fallback (yene de acar telab etmir)
            return searchAniList(query, "MOVIE");
        }

        // açar yalnız URL daxilində gedir, heç bir loga yazılmır
        String url = props.getTmdb().getBaseUrl() + "/search/movie?query="
                + ExternalHttpClient.encode(query) + "&api_key=" + ExternalHttpClient.encode(key);

        String json = http.getJson(url);
        if (json.isBlank()) {
            return List.of();
        }

        List<SearchHit> out = new ArrayList<>();
        try {
            JsonNode results = mapper.readTree(json).path("results");
            if (!results.isArray()) {
                return List.of();
            }
            for (JsonNode m : results) {
                if (out.size() >= LIMIT) {
                    break;
                }
                String title = emptyToNull(m.path("title").asText(null));
                if (title == null) {
                    continue;
                }
                String path = emptyToNull(m.path("poster_path").asText(null));
                out.add(SearchHit.builder()
                        .externalId(m.path("id").isMissingNode() ? null : String.valueOf(m.path("id").asLong()))
                        .title(title)
                        .year(yearOf(m.path("release_date").asText(null)))
                        .posterUrl(path == null ? null : props.getTmdb().getImageBase() + path)
                        .score(normalizeScore(m.path("vote_average").asDouble()))
                        .extra(null)
                        .build());
            }
        } catch (Exception e) {
            log.warn("TMDB cavabı oxunmadı: {}", e.getMessage());
            return List.of();
        }
        return out;
    }

    // ---------------- yardimci ----------------

    private String yearOf(String releaseDate) {
        if (releaseDate != null && releaseDate.length() >= 4) {
            return releaseDate.substring(0, 4);
        }
        return null;
    }

    /** AniList 0-100, TMDB 0-10 -> hər ikisi 0-100. */
    private String normalizeScore(double raw) {
        if (raw <= 0) {
            return null;
        }
        return String.valueOf((int) Math.round(raw));
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return (b != null && !b.isBlank()) ? b : null;
    }

    private String emptyToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    public record SearchHit(String externalId, String title, String year, String posterUrl,
                            String score, String extra) {
        public static SearchHitBuilder builder() {
            return new SearchHitBuilder();
        }

        public static final class SearchHitBuilder {
            private String externalId, title, year, posterUrl, score, extra;

            public SearchHitBuilder externalId(String v) { externalId = v; return this; }
            public SearchHitBuilder title(String v) { title = v; return this; }
            public SearchHitBuilder year(String v) { year = v; return this; }
            public SearchHitBuilder posterUrl(String v) { posterUrl = v; return this; }
            public SearchHitBuilder score(String v) { score = v; return this; }
            public SearchHitBuilder extra(String v) { extra = v; return this; }
            public SearchHit build() {
                return new SearchHit(externalId, title, year, posterUrl, score, extra);
            }
        }
    }
}