package com.wildcard.gamification.dto;

import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One line of the XP history (doc 4.3). Every row is an immutable entry
 * from XpLog - XP can only be read here, never edited (doc 11).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpLogResponse {

    private Long id;
    private XpAction action;
    private XpCategory category;
    private int amount;
    private LocalDateTime createdAt;
}
