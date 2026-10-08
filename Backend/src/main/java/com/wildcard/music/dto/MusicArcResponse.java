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
    /** Checked in today - only computed on the user's own profile. */
    private boolean checkedInToday;
    private int streakDays;
    private long totalCheckIns;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
