package com.wildcard.music.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Başqa istifadəçinin profilində göstərilən musiqi möhürü.
 */
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
    /** Bu user-ə qoyulmuş reaksiyaların sayı. */
    private int reactionCount;
}
