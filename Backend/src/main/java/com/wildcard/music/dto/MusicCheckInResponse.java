package com.wildcard.music.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MusicCheckInResponse {

    private Long arcId;
    private String trackName;
    private String artist;
    private LocalDate date;
    private boolean alreadyCheckedIn;
    private int streakDays;
    private Integer totalCheckIns;
    private String message;
}
