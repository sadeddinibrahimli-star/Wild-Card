package com.wildcard.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * WebSocket yayını üçün təhlükəsiz köməkçi (doc 4.3 real-time notifications).
 *
 * Mesaj yalnız DB-yə yazıldıqdan (commit) SONRA göndərilir ki, REST cavabını
 * oxuyan istifadəçi həmin yazını artıq görə bilsin. WS uğursuz olsa REST axını
 * heç vaxt pozmur - hər şey try/catch içindədir; bağlantı yoxdursa broker
 * sadəcə heç nə etmir (köhnə polling işləməyə davam edir).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsPush {

    private final SimpMessagingTemplate messaging;

    /**
     * Transaksiya içindəsə commit-dən sonra, deyilsə dərhal göndər.
     *
     * @param destination məs. "/topic/chat/5" və ya "/user/5/queue/alerts"
     * @param payload     JSON-a çevriləcək obyekt
     */
    public void afterCommit(String destination, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(destination, payload);
                }
            });
        } else {
            send(destination, payload);
        }
    }

    private void send(String destination, Object payload) {
        try {
            messaging.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.debug("WS push {} uğursuz oldu: {}", destination, e.getMessage());
        }
    }
}
