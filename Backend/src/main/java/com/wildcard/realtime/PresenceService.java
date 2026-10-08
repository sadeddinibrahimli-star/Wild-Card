package com.wildcard.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kim kimi "onlayn" göstərir.
 * Yalnız eyni konfiqurasiyalı instance-da saxlanılır (lokal demo üçün kifayətdir).
 */
@Slf4j
@Service
public class PresenceService {

    /** userId -> session sayı */
    private final Map<Long, Integer> online = new ConcurrentHashMap<>();
    /** userId -> "Yazır..." başladığı an */
    private final Map<Long, Instant> typingSince = new ConcurrentHashMap<>();

    public void userConnected(Long userId) {
        online.merge(userId, 1, Integer::sum);
        log.debug("WS connected: user={} online={}", userId, online.get(userId));
    }

    public void userDisconnected(Long userId) {
        online.computeIfPresent(userId, (k, v) -> v <= 1 ? null : v - 1);
        typingSince.remove(userId);
        log.debug("WS disconnected: user={} online={}", userId, online.get(userId));
    }

    public void onDisconnect(SessionDisconnectEvent event) {
        // sessiya bağlananda onun user id-si artıq silinib (WS config də edir)
        if (event.getUser() != null) {
            log.debug("WS session closed: {}", event.getUser().getName());
        }
    }

    public void setTyping(Long userId) {
        typingSince.put(userId, Instant.now());
    }

    public void clearTyping(Long userId) {
        typingSince.remove(userId);
    }

    public boolean isOnline(Long userId) {
        return online.containsKey(userId);
    }

    public Set<Long> onlineUsers() {
        return Set.copyOf(online.keySet());
    }

    public Long typingSinceEpoch(Long userId) {
        Instant i = typingSince.get(userId);
        return i == null ? null : i.toEpochMilli();
    }
}