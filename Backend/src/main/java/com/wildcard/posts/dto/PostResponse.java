package com.wildcard.posts.dto;

import com.wildcard.posts.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    public record TopicDto(Long id, String slug, String label) {
    }


    private Long id;
    private Long authorId;
    private String authorUsername;
    private String authorAvatarUrl;
    private int authorLevel;
    private String authorRarity;
    private Category category;
    private String title;
    private String body;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private long commentCount;
    private Map<String, Long> reactionCounts;
    private String myReaction;

    private List<TopicDto> topics;
}
