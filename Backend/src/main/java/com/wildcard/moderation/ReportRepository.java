package com.wildcard.moderation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);

    @Query("""
            select r from Report r
            where r.post.author.id = :userId or r.comment.author.id = :userId
            order by r.createdAt desc
            """)
    Page<Report> findFlaggedContentForUser(@Param("userId") Long userId, Pageable pageable);
}
