package com.wildcard.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Discover səhifəsi üçün yüngül istifadəçi kartı.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoverUserResponse {

    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;
    private int level;
    private String rarity;
    private String title;
    private int currentStreak;
    private long totalXp;
    private Instant lastActiveAt;
    /** Niyə bu istifadəçi təklif olunur. */
    private String reason;
    private boolean isFollowing;
}
