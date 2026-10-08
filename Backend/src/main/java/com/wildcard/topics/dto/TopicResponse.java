package com.wildcard.topics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicResponse {

    private Long id;
    private String slug;
    private String label;
    private String category;
    private Double affinity;
    private boolean explicit;
}
