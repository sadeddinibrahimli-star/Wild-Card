package com.wildcard.realtime;

import com.wildcard.chat.ChatMessageRepository;
import com.wildcard.chat.ChatService;
import com.wildcard.chat.dto.SendMessageRequest;
import com.wildcard.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Real-time chat axını.
 *
 *  Mesaj göndərmə:  REST ( mövcud POST ) + STOMP /app/chat/{id}/send
 *  "Yazır…":       /app/chat/{id}/typing   -> /topic/chat/{id}/typing
 *  "Seen · vaxt":  /app/chat/{id}/seen     -> /topic/chat/{id}/seen
 */
@Controller
@RequiredArgsConstructor
public class ChatRealtimeController {

    private final SimpMessagingTemplate messaging;
    private final PresenceService presence;
    private final ChatService chatService;
    private final ChatMessageRepository messageRepository;

    /** Mesajı göndərir - canlı yayın ChatService.send içindədir (həm REST, həm STOMP yolu). */
    @MessageMapping("/chat/{conversationId}/send")
    public void send(@DestinationVariable Long conversationId,
                     @Payload SendMessageRequest request,
                     java.security.Principal principal) {

        chatService.send(current(principal), conversationId, request);
    }

    /** "Yazır…" göstərgəsi - 5 saniyə sonra avtomatik sönmək frontend-un işidir. */
    @MessageMapping("/chat/{conversationId}/typing")
    public void typing(@DestinationVariable Long conversationId, java.security.Principal principal) {
        Long userId = current(principal).getId();
        presence.setTyping(userId);

        messaging.convertAndSend("/topic/chat/" + conversationId + "/typing",
                java.util.Map.of(
                        "userId", userId,
                        "typing", true,
                        "at", java.time.Instant.now().toString()));
    }

    @MessageMapping("/chat/{conversationId}/stop-typing")
    public void stopTyping(@DestinationVariable Long conversationId, java.security.Principal principal) {
        Long userId = current(principal).getId();
        presence.clearTyping(userId);

        messaging.convertAndSend("/topic/chat/" + conversationId + "/typing",
                java.util.Map.of("userId", userId, "typing", false));
    }

    /** Konversiyanı açan istifadəçi bütün mesajları oxunmuş işarələyir. */
    @MessageMapping("/chat/{conversationId}/seen")
    public void seen(@DestinationVariable Long conversationId, java.security.Principal principal) {
        Long userId = current(principal).getId();
        int marked = messageRepository.markRead(conversationId, userId, LocalDateTime.now());

        if (marked > 0) {
            messaging.convertAndSend("/topic/chat/" + conversationId + "/seen",
                    java.util.Map.of(
                            "userId", userId,
                            "readAt", java.time.Instant.now().toString()));
        }
    }

    /** Ayrılma anında " Seen · vaxt" yayılır. */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        presence.onDisconnect(event);
    }

    @MessageMapping("/presence/who")
    public void who() {
        List<Long> online = presence.onlineUsers().stream().sorted().toList();
        messaging.convertAndSend("/queue/presence", java.util.Map.of("onlineUserIds", online));
    }

    /** STOMP mesajlarında SecurityContext olmur - principal-dan istifadə edirik. */
    private com.wildcard.users.User current(java.security.Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof com.wildcard.users.User user) {
            return user;
        }
        throw new com.wildcard.common.BusinessException("Authentication required");
    }
}