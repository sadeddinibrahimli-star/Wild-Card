package com.wildcard.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardRowResponse {

    private int rank;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String rarity;
    private String title;
    private long weeklyXp;
}
