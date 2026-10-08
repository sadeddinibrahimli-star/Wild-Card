package com.wildcard.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long id;
    private String type;
    private String title;
    private Long createdById;
    private long memberCount;
    private LocalDateTime createdAt;
    private List<MemberDto> members;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    public record MemberDto(Long id, String username, String avatarUrl) {
    }
}
