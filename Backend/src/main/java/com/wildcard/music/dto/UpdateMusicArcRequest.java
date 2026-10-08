package com.wildcard.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMusicArcRequest {

    @NotBlank
    @Size(max = 255)
    private String artist;

    @NotBlank
    @Size(max = 255)
    private String trackName;

    @Size(max = 500)
    private String note;

    /** iTunes artworkUrl. */
    @Size(max = 500)
    private String albumArtUrl;
}
