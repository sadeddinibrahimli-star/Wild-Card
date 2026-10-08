package com.wildcard.watchlist;

import com.wildcard.common.BusinessException;
import com.wildcard.common.ContentSanitizer;
import com.wildcard.common.NotFoundException;
import com.wildcard.common.PageResponse;
import com.wildcard.common.RateLimiter;
import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpCategory;
import com.wildcard.gamification.XpService;
import com.wildcard.users.User;
import com.wildcard.watchlist.dto.CreateWatchlistItemRequest;
import com.wildcard.watchlist.dto.UpdateWatchlistItemRequest;
import com.wildcard.watchlist.dto.WatchlistItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final XpService xpService;
    private final RateLimiter rateLimiter;

    @Transactional
    public WatchlistItemResponse create(User user, CreateWatchlistItemRequest request) {
        rateLimiter.checkWatchlist(user);

        String title = ContentSanitizer.line(request.getTitle(), 255);

        if (watchlistRepository.existsByUserIdAndTitleIgnoreCase(user.getId(), title)) {
            throw new BusinessException("This title is already on your watchlist");
        }

        WatchlistItem saved = watchlistRepository.save(WatchlistItem.builder()
                .user(user)
                .title(title)
                .kind(request.getKind())
                .status(request.getStatus())
                .rating(request.getRating())
                .notes(ContentSanitizer.text(request.getNotes()))
                .posterUrl(request.getPosterUrl())
                .externalId(request.getExternalId())
                .build());

        xpService.grant(user, XpAction.WATCHLIST_UPDATE);

        return toResponse(saved);
    }

    @Transactional
    public WatchlistItemResponse update(User user, Long itemId, UpdateWatchlistItemRequest request) {
        WatchlistItem item = getOwned(user, itemId);

        boolean wasCompleted = item.getStatus() == WatchStatus.COMPLETED;
        boolean becomesCompleted = false;

        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
            becomesCompleted = !wasCompleted && item.getStatus() == WatchStatus.COMPLETED;
        }
        if (request.getRating() != null) {
            item.setRating(request.getRating());
        }
        if (request.getNotes() != null) {
            item.setNotes(ContentSanitizer.text(request.getNotes()));
        }
        if (request.getPosterUrl() != null) {
            item.setPosterUrl(request.getPosterUrl());
        }
        if (request.getExternalId() != null) {
            item.setExternalId(request.getExternalId());
        }

        WatchlistItem saved = watchlistRepository.save(item);

        if (becomesCompleted) {
            // tamamlanan item ucun bonus yalnizca bir dfe
            xpService.grant(user, XpAction.WATCHLIST_COMPLETED,
                    saved.getKind() == MediaKind.ANIME ? XpCategory.ANI : XpCategory.CHA);
        } else {
            xpService.grant(user, XpAction.WATCHLIST_UPDATE);
        }

        return toResponse(saved);
    }

    @Transactional
    public void delete(User user, Long itemId) {
        watchlistRepository.delete(getOwned(user, itemId));
    }

    @Transactional(readOnly = true)
    public PageResponse<WatchlistItemResponse> list(User user, WatchStatus status, Pageable pageable) {
        var page = status == null
                ? watchlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                : watchlistRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        user.getId(), status, pageable);

        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public WatchlistItem getOwned(User user, Long itemId) {
        return watchlistRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new NotFoundException("Watchlist item not found: " + itemId));
    }

    /** BACKEND.md: GET /watchlist/stats - siyahının xülasəsi (statusa görə sayım). */
    @Transactional(readOnly = true)
    public WatchlistStats stats(User user) {
        Long id = user.getId();

        int watching = (int) watchlistRepository.countByUserIdAndStatus(id, WatchStatus.WATCHING);
        int completed = (int) watchlistRepository.countByUserIdAndStatus(id, WatchStatus.COMPLETED);
        int planToWatch = (int) watchlistRepository.countByUserIdAndStatus(id, WatchStatus.PLAN_TO_WATCH);
        int dropped = (int) watchlistRepository.countByUserIdAndStatus(id, WatchStatus.DROPPED);

        long total = watchlistRepository.countByUserId(id);
        long rated = watchlistRepository.countByUserIdAndRatingIsNotNull(id);
        double avg = watchlistRepository.averageRating(id);

        return new WatchlistStats(total, watching, completed, planToWatch, dropped, rated,
                total == 0 ? null : Math.round(avg * 10.0) / 10.0);
    }

    public record WatchlistStats(long total, int watching, int completed, int planToWatch,
                                 int dropped, long rated, Double averageRating) {
    }

    private WatchlistItemResponse toResponse(WatchlistItem item) {
        return WatchlistItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .kind(item.getKind())
                .status(item.getStatus())
                .rating(item.getRating())
                .notes(item.getNotes())
                .posterUrl(item.getPosterUrl())
                .externalId(item.getExternalId())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}