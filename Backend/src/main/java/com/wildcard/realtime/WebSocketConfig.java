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
 * STOMP WebSocket configuration.
 *
 *  Connection: /ws (SockJS supported)
 *   Subscribe:
 *     /topic/chat/{conversationId}   -> new messages
 *     /topic/alerts/{userId}         -> notifications
 *     /queue/presence                -> "Typing..." / "Seen - time"
 *   Publish:
 *     /app/chat/{conversationId}/typing
 *     /app/chat/{conversationId}/seen
 *
 * The JWT arrives at the handshake as "Authorization: Bearer <token>"
 * (the browser WebSocket API cannot send headers, so a STOMP CONNECT
 * frame may also carry an "Authorization" header).
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

                // The SecurityContext is built for every following message,
                // otherwise SecurityUtils.getCurrentUser() would not work.
                var rawAccessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
                Long userId = sessionUserId(accessor);
                if (userId != null) {
                    var auth = userRepository.findById(userId)
                            .map(u -> new UsernamePasswordAuthenticationToken(u, null, List.of()))
                            .orElse(null);

                    if (auth != null) {
                        org.springframework.security.core.context.SecurityContextHolder
                                .getContext().setAuthentication(auth);

                        // Messages after CONNECT are immutable,
                        // a mutable copy is needed to attach the authentication.
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

    /** The user id is read from the session attribute (the principal is an object, so getName() is not used). */
    private Long sessionUserId(StompHeaderAccessor accessor) {
        Map<String, Object> session = accessor.getSessionAttributes();
        Object value = session == null ? null : session.get(USER_ID);
        return value instanceof Number n ? n.longValue() : null;
    }

    private Long authenticate(StompHeaderAccessor accessor) {
        String header = headerValue(accessor, "Authorization");
        if (header == null || !header.toLowerCase().startsWith("bearer ")) {
            // some clients send the token in a separate "token" header
            return parse(headerValue(accessor, "token"));
        }
        return parse(header.substring(7).trim());
    }

    /** STOMP headers are case-insensitive - some clients send them in lower case. */
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