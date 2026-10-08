package com.wildcard.gamification.dto;

import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Doc 4.3: "View own XP history".
 * Hər sətir XpLog-dakı dəyişməz yazıdır - istifadəçi XP-ni yalnız
 * buradan oxuyur, heç vaxt düzəldə bilmir (doc 11).
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
