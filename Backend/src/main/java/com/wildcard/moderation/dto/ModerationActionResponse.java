package com.wildcard.moderation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Moderasiya aksiyasının nəticəsi (gizlətmə / silmə / bərpa).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModerationActionResponse {

    private String action;
    private Long postId;
    private Long commentId;
    private Boolean hidden;
    private String message;
}
