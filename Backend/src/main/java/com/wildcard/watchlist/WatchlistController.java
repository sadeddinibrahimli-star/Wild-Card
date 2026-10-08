package com.wildcard.watchlist;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.PageResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.watchlist.dto.CreateWatchlistItemRequest;
import com.wildcard.watchlist.dto.UpdateWatchlistItemRequest;
import com.wildcard.watchlist.dto.WatchlistItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @PostMapping
    public ApiResponse<WatchlistItemResponse> create(
            @Valid @RequestBody CreateWatchlistItemRequest request) {
        return ApiResponse.success("Added to watchlist",
                watchlistService.create(SecurityUtils.getCurrentUser(), request));
    }

    @GetMapping
    public ApiResponse<PageResponse<WatchlistItemResponse>> list(
            @RequestParam(required = false) WatchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(
                watchlistService.list(SecurityUtils.getCurrentUser(), status, PageRequest.of(page, size)));
    }

    @GetMapping("/stats")
    public ApiResponse<WatchlistService.WatchlistStats> stats() {
        return ApiResponse.success(watchlistService.stats(SecurityUtils.getCurrentUser()));
    }

    @PutMapping("/{itemId}")
    public ApiResponse<WatchlistItemResponse> update(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateWatchlistItemRequest request) {
        return ApiResponse.success("Watchlist updated",
                watchlistService.update(SecurityUtils.getCurrentUser(), itemId, request));
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long itemId) {
        watchlistService.delete(SecurityUtils.getCurrentUser(), itemId);
    }
}