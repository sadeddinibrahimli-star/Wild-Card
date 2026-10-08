package com.wildcard.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Safe helper for WebSocket pushes (doc 4.3 real-time notifications).
 *
 * The message is only sent AFTER the database write (commit) so a user
 * reading the REST response already sees the record. A failing WS never
 * breaks the REST flow - everything is inside try/catch; without a
 * connection the broker simply does nothing (old polling keeps working).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsPush {

    private final SimpMessagingTemplate messaging;

    /**
     * Send after the commit when inside a transaction, immediately otherwise.
     *
     * @param destination e.g. "/topic/chat/5" or "/user/5/queue/alerts"
     * @param payload     object serialized to JSON
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
