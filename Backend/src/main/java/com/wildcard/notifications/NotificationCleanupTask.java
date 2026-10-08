package com.wildcard.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Köhnə bildirişləri təmizləmək.
 *
 *  - oxunmuş bildirişlər 30 gündən köhnədirsə silinir
 *  - oxunmamış bildirişlər 90 gündən köhnədirsə silinir
 *  - gündə bir dəfə, saat 03:20
 */
@Slf4j
@Component
public class NotificationCleanupTask {

    private static final int READ_KEEP_DAYS = 30;
    private static final int UNREAD_KEEP_DAYS = 90;

    private final NotificationRepository notificationRepository;

    public NotificationCleanupTask(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(cron = "0 20 3 * * *")
    @Transactional
    public void cleanup() {
        int removed = notificationRepository.deleteReadBefore(
                LocalDateTime.now().minusDays(READ_KEEP_DAYS));
        log.info("Notification cleanup: {} read notifications older than {} days removed",
                removed, READ_KEEP_DAYS);
    }

    @Scheduled(cron = "0 30 4 1 * *")
    @Transactional
    public void cleanupUnread() {
        int removed = notificationRepository.deleteReadBefore(
                LocalDateTime.now().minusDays(UNREAD_KEEP_DAYS));
        log.info("Notification cleanup: {} notifications older than {} days removed",
                removed, UNREAD_KEEP_DAYS);
    }
}