package com.wildcard.reactions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummaryResponse {

    private String myReaction;
    private long total;
    private Map<String, Long> countsByType;
}
