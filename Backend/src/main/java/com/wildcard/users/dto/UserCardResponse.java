package com.wildcard.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCardResponse {

    private Long id;
    private String username;
    private String avatarUrl;
    private String title;
    private String flavorText;
    private String rarity;
    private int level;

    private int ani;
    private int gam;
    private int mus;
    private int cha;

    private long totalXp;
    private int xpIntoLevel;
    private int xpForNext;

    private int currentStreak;
    private int longestStreak;
}
