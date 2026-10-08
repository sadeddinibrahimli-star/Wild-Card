package com.wildcard.music.dto;

import com.wildcard.reactions.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MusicArcReactionRequest {

    @NotNull
    private ReactionType type;
}
