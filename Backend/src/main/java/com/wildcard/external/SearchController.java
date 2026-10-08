package com.wildcard.external;

import com.wildcard.common.ApiResponse;
import com.wildcard.external.MusicSearchClient.MusicHit;
import com.wildcard.external.TitleSearchClient.SearchHit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Search endpoints. The frontend only talks to this backend,
 * never to the external APIs directly.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final TitleSearchClient titles;
    private final MusicSearchClient music;

    /** ANIME -> AniList, FILM -> TMDB (empty key -> empty list). */
    @GetMapping("/titles")
    public ApiResponse<List<SearchHit>> titles(@RequestParam("q") String q,
                                               @RequestParam(defaultValue = "ANIME") String type) {
        return ApiResponse.success(titles.search(q, type));
    }

    /** Tells the frontend whether a TMDB key is configured. */
    @GetMapping("/titles/status")
    public ApiResponse<java.util.Map<String, Object>> status() {
        return ApiResponse.success(java.util.Map.of(
                "filmSearchEnabled", titles.filmSearchEnabled(),
                "animeSource", "AniList",
                "filmSource", "TMDB"));
    }

    @GetMapping("/music")
    public ApiResponse<List<MusicHit>> music(@RequestParam("q") String q) {
        return ApiResponse.success(music.search(q));
    }
}