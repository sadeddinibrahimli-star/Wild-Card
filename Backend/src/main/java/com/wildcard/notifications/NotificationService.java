package com.wildcard.notifications;

import com.wildcard.common.NotFoundException;
import com.wildcard.common.PageResponse;
import com.wildcard.notifications.dto.NotificationResponse;
import com.wildcard.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final com.wildcard.realtime.WsPush wsPush;

    @Transactional(propagation = Propagation.MANDATORY)
    public void notify(User recipient, NotificationType type, String message, Long postId) {
        if (recipient == null) {
            return;
        }

        String text = message.length() > 250 ? message.substring(0, 250) : message;

        Notification saved = notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(text)
                .postId(postId)
                .build());

        // doc 4.3: "Receive real-time notifications" - /user/queue/alerts.
        // Köhnə polling (5s) eyni vaxtda işləməyə davam edir.
        wsPush.afterCommit("/user/" + recipient.getId() + "/queue/alerts", java.util.Map.of(
                "type", "NOTIFICATION",
                "notificationType", type.name(),
                "id", saved.getId(),
                "message", text));
    }

    @Transactional(readOnly = true)
    /** filter: all (default) | follows | likes */
    public PageResponse<NotificationResponse> listFor(User user, Pageable pageable) {
        return listFor(user, pageable, "all");
    }

    /** filter: all | follows | likes */
    public PageResponse<NotificationResponse> listFor(User user, Pageable pageable, String filter) {
        boolean all = filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter);
        var types = switch (filter == null ? "all" : filter.toLowerCase()) {
            case "follows", "follows " -> java.util.List.of(NotificationType.FOLLOW);
            case "likes" -> java.util.List.of(NotificationType.REACTION, NotificationType.COMMENT);
            default -> java.util.List.of(NotificationType.values());
        };
        return PageResponse.from(
                notificationRepository.findFiltered(user.getId(), types, all, pageable)
                        .map(this::toResponse));
    }

    private PageResponse<NotificationResponse> listForLegacy(User user, Pageable pageable) {
        return PageResponse.from(
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageable)
                        .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notificationRepository.countByRecipientIdAndReadFalse(user.getId());
    }

    @Transactional
    public void markRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new NotFoundException("Notification not found: " + notificationId);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(User user) {
        var page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                user.getId(), Pageable.unpaged());

        page.stream()
                .filter(notification -> !notification.isRead())
                .forEach(notification -> notification.setRead(true));

        notificationRepository.saveAll(page);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .message(notification.getMessage())
                .postId(notification.getPostId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}