package com.wildcard.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyPositionResponse {

    private boolean ranked;
    private int rank;
    private long weeklyXp;
    private long totalXp;
}
