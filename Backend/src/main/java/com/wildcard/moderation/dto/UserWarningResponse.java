package com.wildcard.moderation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWarningResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long moderatorId;
    private String moderatorUsername;
    private String reason;
    private LocalDateTime createdAt;
}
