package com.wildcard.chat;

import com.wildcard.chat.dto.ChatMessageResponse;
import com.wildcard.chat.dto.ConversationResponse;
import com.wildcard.chat.dto.CreateGroupRequest;
import com.wildcard.chat.dto.SendMessageRequest;
import com.wildcard.common.BusinessException;
import com.wildcard.common.ContentSanitizer;
import com.wildcard.common.NotFoundException;
import com.wildcard.common.PageResponse;
import com.wildcard.common.RateLimiter;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RateLimiter rateLimiter;
    private final com.wildcard.realtime.WsPush wsPush;

    @Transactional
    public ConversationResponse openDirect(User current, Long otherUserId) {
        if (current.getId().equals(otherUserId)) {
            throw new BusinessException("You cannot start a chat with yourself");
        }

        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new NotFoundException("User not found: " + otherUserId));

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .createdById(current.getId())
                .build());

        memberRepository.save(ConversationMember.builder()
                .conversation(conversation).user(current).hasLeft(false).build());
        memberRepository.save(ConversationMember.builder()
                .conversation(conversation).user(other).hasLeft(false).build());

        return toResponse(conversation, List.of(current, other), null, null, current);
    }

    @Transactional
    public ConversationResponse createGroup(User current, CreateGroupRequest request) {
        Conversation conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.GROUP)
                .title(ContentSanitizer.line(request.getTitle(), 120))
                .createdById(current.getId())
                .build());

        memberRepository.save(ConversationMember.builder()
                .conversation(conversation).user(current).hasLeft(false).build());

        if (request.getMemberIds() != null) {
            for (User member : userRepository.findAllById(request.getMemberIds())) {
                if (!member.getId().equals(current.getId())) {
                    memberRepository.save(ConversationMember.builder()
                            .conversation(conversation).user(member).hasLeft(false).build());
                }
            }
        }

        return getConversation(current, conversation.getId());
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> myConversations(User current) {
        List<ConversationMember> memberships = memberRepository.findActiveByUserId(current.getId());

        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> conversationIds = memberships.stream()
                .map(member -> member.getConversation().getId())
                .toList();

        List<Long> memberUserIds = memberRepository.findAll().stream()
                .filter(member -> conversationIds.contains(member.getConversation().getId()))
                .filter(member -> !member.isHasLeft())
                .map(member -> member.getUser().getId())
                .distinct()
                .toList();

        Map<Long, User> users = userRepository.findAllById(memberUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ConversationResponse> responses = new ArrayList<>();

        for (ConversationMember membership : memberships) {
            Conversation conversation = membership.getConversation();

            List<User> members = memberRepository
                    .findAll().stream()
                    .filter(member -> member.getConversation().getId().equals(conversation.getId()))
                    .filter(member -> !member.isHasLeft())
                    .map(member -> member.getUser())
                    .toList();

            Page<ChatMessage> lastMessages = messageRepository
                    .findByConversationIdOrderByCreatedAtDesc(conversation.getId(), firstOne());
            ChatMessage last = lastMessages.getContent().isEmpty() ? null : lastMessages.getContent().get(0);

            responses.add(toResponse(conversation, members, last, users, current));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(User current, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));

        requireMember(current, conversationId);

        List<User> members = memberRepository.findAll().stream()
                .filter(member -> member.getConversation().getId().equals(conversationId))
                .filter(member -> !member.isHasLeft())
                .map(ConversationMember::getUser)
                .toList();

        Page<ChatMessage> lastMessages = messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, firstOne());
        ChatMessage last = lastMessages.getContent().isEmpty() ? null : lastMessages.getContent().get(0);

        return toResponse(conversation, members, last, Map.of(), current);
    }

    @Transactional
    public ChatMessageResponse send(User current, Long conversationId, SendMessageRequest request) {
        requireMember(current, conversationId);
        rateLimiter.checkComments(current);

        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .conversation(conversationRepository.getReferenceById(conversationId))
                .sender(current)
                .body(ContentSanitizer.richText(request.getBody()))
                .build());

        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(saved.getId())
                .conversationId(conversationId)
                .senderId(current.getId())
                .senderUsername(current.getUsername())
                .body(saved.getBody())
                .createdAt(saved.getCreatedAt())
                .readAt(null)
                .build();

        // doc 4.3 / 6: canlı chat - açıq söhbət anında yenilənir
        wsPush.afterCommit("/topic/chat/" + conversationId, response);

        // digər üzvlərə alert (onların brauzeri açıqdırsa wc:alerts işə düşür)
        memberRepository.findAll().stream()
                .filter(member -> member.getConversation().getId().equals(conversationId))
                .filter(member -> !member.isHasLeft())
                .map(member -> member.getUser().getId())
                .filter(memberId -> !memberId.equals(current.getId()))
                .forEach(memberId -> wsPush.afterCommit(
                        "/user/" + memberId + "/queue/alerts",
                        Map.of("type", "CHAT_MESSAGE", "conversationId", conversationId)));

        return response;
    }

    @Transactional
    public PageResponse<ChatMessageResponse> messages(User current, Long conversationId, Pageable pageable) {
        requireMember(current, conversationId);

        // mesajlar acilan kimi oxunmus isare olunur
        messageRepository.markRead(conversationId, current.getId(), LocalDateTime.now());

        return PageResponse.from(
                messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                        .map(this::toMessageResponse));
    }

    private void requireMember(User current, Long conversationId) {
        memberRepository.findByConversationIdAndUserId(conversationId, current.getId())
                .filter(member -> !member.isHasLeft())
                .orElseThrow(() -> new NotFoundException(
                        "Conversation not found: " + conversationId));
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .body(message.getBody())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .build();
    }

    private ConversationResponse toResponse(Conversation conversation, List<User> members,
                                            ChatMessage last, Map<Long, User> users, User current) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType().name())
                .title(conversation.getTitle())
                .createdById(conversation.getCreatedById())
                .memberCount(memberRepository.countByConversationIdAndHasLeftFalse(conversation.getId()))
                .createdAt(conversation.getCreatedAt())
                .members(members.stream()
                        .map(user -> new ConversationResponse.MemberDto(
                                user.getId(), user.getUsername(), user.getAvatarUrl()))
                        .toList())
                .lastMessage(last == null ? null : last.getBody())
                .lastMessageAt(last == null ? null : last.getCreatedAt())
                .unreadCount(current == null ? 0 : messageRepository.countUnread(conversation.getId(), current.getId()))
                .build();
    }

    private PageRequest firstOne() {
        return PageRequest.of(0, 1);
    }

    /** Spec: PUT /chat/{id}/read - oxunmuş işarələyir, qalan oxunmamış sayını qaytarır. */
    @Transactional
    public long markRead(User current, Long conversationId) {
        requireMember(current, conversationId);
        messageRepository.markRead(conversationId, current.getId(), LocalDateTime.now());
        return messageRepository.countUnread(conversationId, current.getId());
    }
}
