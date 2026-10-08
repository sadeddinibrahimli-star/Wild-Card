package com.wildcard.achievements.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementResponse {

    private String code;
    private String name;
    private String title;
    private String description;
    private String rarity;    // COMMON | RARE | EPIC | LEGENDARY
    private String icon;      // line icon key for the frontend
    private boolean unlocked;
    /** Badge nə vaxt açıldı (unlocked deyilsə null). */
    private java.time.Instant unlockedAt;
    /** null when the achievement is not calculated yet. */
    private Integer current;
    private Integer target;
    private Integer max;
    /** true when the backend has no rule for this one yet. */
    private boolean comingSoon;
    private boolean available = true;
}
