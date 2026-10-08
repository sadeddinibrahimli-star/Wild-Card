package com.wildcard.achievements.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityResponse {

    private Long userId;
    private Long otherId;
    private String otherUsername;
    private String otherAvatarUrl;

    private int score;

    private int overall;
    private int anime;
    private int gaming;
    private int music;
    private int chaos;

    private List<StatPair> stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatPair {
        private String key;
        private String label;
        private int mine;
        private int theirs;
        private int score;
    }
}
