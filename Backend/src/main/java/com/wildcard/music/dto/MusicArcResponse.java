package com.wildcard.music.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicArcResponse {

    private Long id;
    private String artist;
    private String trackName;
    private String note;
    private String albumArtUrl;
    private boolean current;
    /** Bu gün check-in olunub-olmadığı (yalnız öz profilində hesablanır). */
    private boolean checkedInToday;
    /** Ardıcı gün sayı. */
    private int streakDays;
    /** Bu istifadəçinin ümumi check-in sayı. */
    private long totalCheckIns;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
