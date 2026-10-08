package com.wildcard.gamification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface XpLogRepository extends JpaRepository<XpLog, Long> {

    List<XpLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<XpLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("select l.category, sum(l.amount) from XpLog l where l.user.id = :userId group by l.category")
    List<Object[]> sumByCategory(@Param("userId") Long userId);

    @Query("select coalesce(sum(l.amount), 0) from XpLog l")
    long totalXpGranted();

    @Query("select coalesce(sum(l.amount), 0) from XpLog l where l.createdAt >= :since")
    long totalXpGrantedSince(@Param("since") LocalDateTime since);

    @Query("""
            select l.user.id, l.user.username, sum(l.amount)
            from XpLog l
            where l.createdAt >= :since
            group by l.user.id, l.user.username
            order by sum(l.amount) desc
            """)
    List<Object[]> weeklyRanking(@Param("since") LocalDateTime since);

    @Query("""
            select count(distinct l.user.id) from XpLog l where l.createdAt >= :since
            """)
    long activeUserCountSince(@Param("since") LocalDateTime since);

    long countByUserIdAndActionAndCreatedAtAfter(Long userId, XpAction action,
                                                 LocalDateTime since);
}