package com.wildcard.common;

import com.wildcard.users.User;
import lombok.extern.slf4j.Slf4j;
import com.wildcard.config.WildcardProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimiter {

    private record Window(long startedAt, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private final int postLimit;
    private final int commentLimit;
    private final int watchlistLimit;
    private final long windowSeconds;

    public RateLimiter(WildcardProperties props) {
        var rl = props.getRateLimit();
        this.postLimit = rl.getPostsPerWindow();
        this.commentLimit = rl.getCommentsPerWindow();
        this.watchlistLimit = rl.getWatchlistPerWindow();
        this.windowSeconds = rl.getWindowSeconds();
    }

    public void checkPosts(User user) {
        check(user, "post", postLimit);
    }

    public void checkComments(User user) {
        check(user, "comment", commentLimit);
    }

    public void checkWatchlist(User user) {
        check(user, "watchlist", watchlistLimit);
    }

    private void check(User user, String action, int limit) {
        String key = user.getId() + ":" + action;
        long now = Instant.now().getEpochSecond();

        Window updated = windows.compute(key, (ignored, current) -> {
            long windowStart = now - windowSeconds;

            if (current == null || current.startedAt() <= windowStart) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.count() + 1);
        });

        if (updated.count() > limit) {
            long retryAfter = windowSeconds - (now - updated.startedAt());
            throw new BusinessException(
                    "You are going too fast. Try again in " + Math.max(1, retryAfter) + "s.");
        }
    }

    @Scheduled(fixedDelay = 300_000)
    void evictStaleWindows() {
        long cutoff = Instant.now().getEpochSecond() - windowSeconds;
        windows.entrySet().removeIf(entry -> entry.getValue().startedAt() <= cutoff);
    }
}