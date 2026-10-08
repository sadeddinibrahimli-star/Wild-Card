package com.wildcard.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    /** Why this user is suggested. */
    private String reason;
    private boolean isFollowing;
}
