package com.wildcard.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("""
            select m from ConversationMember m
            join fetch m.conversation c
            where m.user.id = :userId and m.hasLeft = false
            order by c.createdAt desc
            """)
    List<ConversationMember> findActiveByUserId(@Param("userId") Long userId);

    long countByConversationIdAndHasLeftFalse(Long conversationId);
}
