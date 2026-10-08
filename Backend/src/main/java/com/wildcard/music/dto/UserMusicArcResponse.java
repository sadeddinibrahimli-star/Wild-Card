package com.wildcard.music.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Music badge shown on another user's profile. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMusicArcResponse {

    private Long userId;
    private String username;
    private Long arcId;
    private String artist;
    private String trackName;
    private String albumArtUrl;
    private String note;
    private LocalDate startedOn;
    private boolean checkedInToday;
    private int streakDays;
    private List<LocalDate> recentCheckIns;
    private int reactionCount;
}
