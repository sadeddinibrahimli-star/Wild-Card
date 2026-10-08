package com.wildcard.watchlist.dto;

import com.wildcard.watchlist.MediaKind;
import com.wildcard.watchlist.WatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistItemResponse {

    private Long id;
    private String title;
    private MediaKind kind;
    private WatchStatus status;
    private Integer rating;
    private String notes;
    private String externalId;
    private String posterUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
