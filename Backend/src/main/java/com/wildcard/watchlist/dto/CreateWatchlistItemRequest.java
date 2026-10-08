package com.wildcard.watchlist.dto;

import com.wildcard.watchlist.MediaKind;
import com.wildcard.watchlist.WatchStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWatchlistItemRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    private MediaKind kind;

    @NotNull
    private WatchStatus status;

    @Min(1)
    @Max(10)
    private Integer rating;

    @Size(max = 1000)
    private String notes;

    @Size(max = 500)
    private String posterUrl;

    @Size(max = 100)
    private String externalId;
}