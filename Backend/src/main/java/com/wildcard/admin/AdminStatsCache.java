package com.wildcard.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Platform statistics, computed at 02:00 and cached.
 *
 * So the dashboard does not run heavy COUNT queries on every request:
 *  - computed at 02:00
 *  - otherwise served with a 15 minute TTL when nobody refreshes
 *  - once refreshed it keeps the pre-02:00 value (crash-safe)
 */
@Slf4j
@Component
public class AdminStatsCache {

    private static final long TTL_MILLIS = 15 * 60 * 1000L;

    private final AdminStatsService statsService;
    private final AtomicReference<Map<String, Object>> cached = new AtomicReference<>();
    private volatile long refreshedAt = 0L;

    public AdminStatsCache(AdminStatsService statsService) {
        this.statsService = statsService;
    }

    public Map<String, Object> get() {
        long now = System.currentTimeMillis();
        Map<String, Object> snapshot = cached.get();

        if (snapshot != null && now - refreshedAt < TTL_MILLIS) {
            return withMeta(snapshot, false);
        }
        return withMeta(refreshNow(), true);
    }

    public Map<String, Object> refreshNow() {
        try {
            Map<String, Object> fresh = statsService.platformStats();
            cached.set(fresh);
            refreshedAt = System.currentTimeMillis();
            return fresh;
        } catch (Exception e) {
            log.warn("Stats cache refresh failed, keeping previous snapshot: {}", e.getMessage());
            Map<String, Object> previous = cached.get();
            return previous != null ? previous : Map.of("error", "stats unavailable");
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void nightlyRefresh() {
        log.info("Refreshing platform stats cache (scheduled 02:00)");
        refreshNow();
    }

    private Map<String, Object> withMeta(Map<String, Object> stats, boolean live) {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>(stats);
        out.put("cachedAt", Instant.ofEpochMilli(refreshedAt).toString());
        out.put("live", live);
        return out;
    }
}