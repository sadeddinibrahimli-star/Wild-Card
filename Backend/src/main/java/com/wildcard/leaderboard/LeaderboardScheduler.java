package com.wildcard.leaderboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardScheduler {

    private final LeaderboardService leaderboardService;

    @Scheduled(cron = "0 0 3 * * *")
    public void refreshNightly() {
        recalculate("nightly job");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        recalculate("startup");
    }

    private void recalculate(String reason) {
        try {
            leaderboardService.recalculate();
        } catch (Exception ex) {
            log.error("Leaderboard refresh failed ({})", reason, ex);
        }
    }
}
