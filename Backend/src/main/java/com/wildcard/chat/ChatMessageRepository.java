package com.wildcard.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    long countByConversationId(Long conversationId);

    @org.springframework.data.jpa.repository.Query("""
            select count(m) from ChatMessage m
            where m.conversation.id = :conversationId
              and m.readAt is null
              and m.sender.id <> :userId
            """)
    long countUnread(@org.springframework.data.repository.query.Param("conversationId") Long conversationId,
                     @org.springframework.data.repository.query.Param("userId") Long userId);

    @Modifying
    @Query("""
            update ChatMessage m set m.readAt = :now
            where m.conversation.id = :conversationId
              and m.readAt is null
              and m.sender.id <> :userId
            """)
    int markRead(@Param("conversationId") Long conversationId,
                @Param("userId") Long userId,
                @Param("now") java.time.LocalDateTime now);
}
