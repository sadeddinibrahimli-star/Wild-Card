package com.wildcard.external;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Simple sliding-window rate limiter.
 * AniList official limit: 90 requests per minute.
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