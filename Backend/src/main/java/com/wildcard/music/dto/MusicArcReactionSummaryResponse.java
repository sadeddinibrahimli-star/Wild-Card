package com.wildcard.music.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wildcard.reactions.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MusicArcReactionSummaryResponse {

    private Long arcId;
    private Integer total;
    /** FIRE -> 3, HEART -> 1 ... */
    private Map<ReactionType, Integer> counts;
    /** Bu istifadəçinin öz reaksiyası (yokdursa null). */
    private ReactionType myReaction;
}
