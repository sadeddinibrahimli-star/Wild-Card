package com.wildcard.notifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    /** Oxunmus və köhnə bildirişləri təmizləmək üçün. */
    @Modifying
    @Query("delete from Notification n where n.read = true and n.createdAt < :cutoff")
    int deleteReadBefore(@Param("cutoff") java.time.LocalDateTime cutoff);

    /**
     * filter: all | follows | likes
     *  - follows -> FOLLOW bildirişləri
     *  - likes   -> REACTION / COMMENT bildirişləri
     */
    @Query("""
            select n from Notification n
            where n.recipient.id = :recipientId
              and (:allTypes = true or n.type in :types)
            order by n.createdAt desc
            """)
    Page<Notification> findFiltered(@Param("recipientId") Long recipientId,
                                     @Param("types") Collection<NotificationType> types,
                                     @Param("allTypes") boolean allTypes,
                                     Pageable pageable);
}
