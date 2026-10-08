package com.wildcard.external;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sürüşdərmə pəncərəsi (sliding window) sadə limiter.
 * AniList rəsmi limiti: dəqiqədə 90 request.
 */
final class AniListGuard {

    private final int limit;
    private final Deque<Instant> hits = new ArrayDeque<>();

    AniListGuard(int limit) {
        this.limit = limit;
    }

    synchronized boolean allow() {
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(60);
        while (!hits.isEmpty() && hits.peekFirst().isBefore(cutoff)) {
            hits.pollFirst();
        }
        if (hits.size() >= limit) {
            return false;
        }
        hits.addLast(now);
        return true;
    }
}