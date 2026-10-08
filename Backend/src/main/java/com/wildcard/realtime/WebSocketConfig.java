package com.wildcard.realtime;

import com.wildcard.auth.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.Map;

/**
 * STOMP WebSocket konfiqurasiyası.
 *
 * Əlaqə: /ws (SockJS dəstəklənir)
 *  Subscribe:
 *    /topic/chat/{conversationId}   -> yeni mesajlar
 *    /topic/alerts/{userId}         -> bildirişlər
 *    /queue/presence                -> "Yazır..." / "Seen · vaxt"
 *  Publish:
 *    /app/chat/{conversationId}/typing
 *    /app/chat/{conversationId}/seen
 *
 * JWT handshake zamanında "Authorization: Bearer <token>" başlığı ilə gəlir
 * (browser WebSocket API başlıq göndərə bilmədiyi üçün
 *  STOMP CONNECT çərçivəsində "Authorization" header də qəbul olunur).
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final PresenceService presenceService;
    private final com.wildcard.users.UserRepository userRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                // CONNECT çərçivəsində token yoxdursa bağlantı rədd edilir
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Long userId = authenticate(accessor);
                    if (userId == null) {
                        throw new org.springframework.security.authentication.BadCredentialsException(
                                "Invalid or missing token");
                    }
                    Map<String, Object> session = accessor.getSessionAttributes();
                    if (session != null) {
                        session.put(USER_ID, userId);
                    }
                    presenceService.userConnected(userId);
                }

                // Sonrakı hər mesaj üçün SecurityContext qurulur,
                // yoxsa SecurityUtils.getCurrentUser() işləməz.
                var rawAccessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
                Long userId = sessionUserId(accessor);
                if (userId != null) {
                    var auth = userRepository.findById(userId)
                            .map(u -> new UsernamePasswordAuthenticationToken(u, null, List.of()))
                            .orElse(null);

                    if (auth != null) {
                        org.springframework.security.core.context.SecurityContextHolder
                                .getContext().setAuthentication(auth);
                        // controller-lər Principal qəbul edir

                        // CONNECT-dan sonrakı mesajlar immutable-dır,
                        // dəyişiklik etmək üçün mutable kopya lazımdır.
                        if (rawAccessor instanceof org.springframework.messaging.simp
                                .SimpMessageHeaderAccessor simp && simp.isMutable()) {
                            simp.setUser(auth);
                        }
                        return message;
                    }
                }

                if (StompCommand.DISCONNECT.equals(accessor.getCommand()) && userId != null) {
                    presenceService.userDisconnected(userId);
                }
                return message;
            }
        });
    }

    private static final String USER_ID = "wildcard.userId";

    /** User id sessiya atributundan oxunur (principal obyekt olur, ona görə getName etmirik). */
    private Long sessionUserId(StompHeaderAccessor accessor) {
        Map<String, Object> session = accessor.getSessionAttributes();
        Object value = session == null ? null : session.get(USER_ID);
        return value instanceof Number n ? n.longValue() : null;
    }

    /** Authorization başlığından JWT-ni oxuyur və user id qaytarır. */
    private Long authenticate(StompHeaderAccessor accessor) {
        String header = headerValue(accessor, "Authorization");
        if (header == null || !header.toLowerCase().startsWith("bearer ")) {
            // bəzən STOMP başlığı bütün mətn kimi gəlir
            return parse(headerValue(accessor, "token"));
        }
        return parse(header.substring(7).trim());
    }

    /** STOMP başlıqları həssasdır - bəzi müştərilər kiçik hərf göndərir. */
    private String headerValue(StompHeaderAccessor accessor, String name) {
        String direct = accessor.getFirstNativeHeader(name);
        if (direct != null) {
            return direct;
        }
        if ("Authorization".equalsIgnoreCase(name)) {
            return accessor.getFirstNativeHeader("authorization");
        }
        if ("token".equalsIgnoreCase(name)) {
            return accessor.getFirstNativeHeader("Token");
        }
        return null;
    }

    private Long parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return jwtService.extractUserId(token);
        } catch (Exception e) {
            return null;
        }
    }
}