package com.wildcard.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;

    /** Yalnız öz profil cavabında doldurulur (GET/PUT /users/me) — publik profildə null. */
    private String email;

    private String role;
    private String accountStatus;
    private LocalDateTime createdAt;
    private java.time.Instant lastActiveAt;

    private int ani;
    private int gam;
    private int mus;
    private int cha;
    private long totalXp;
    private int level;
    private String rarity;
    private String title;

    private int currentStreak;
    private int longestStreak;
    private boolean onboarded;
    private boolean isFollowing;
}