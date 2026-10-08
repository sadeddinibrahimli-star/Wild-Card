package com.wildcard.music;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MusicArcRepository extends JpaRepository<MusicArc, Long> {

    Optional<MusicArc> findByUserIdAndCurrentTrue(Long userId);

    Page<MusicArc> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MusicArc a
            set a.current = false, a.endedAt = :now
            where a.user.id = :userId and a.current = true
            """)
    int archiveCurrent(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Musiqi arc-lərinin başlanma anları (ON REPEAT üçün).
     * function('date', ...) H2-də yoxdur - Java-da konvertasiya edirik.
     */
    @Query("select a.startedAt from MusicArc a " +
           "where a.user.id = :userId and a.startedAt is not null order by a.startedAt desc")
    List<LocalDateTime> startDatesFor(@Param("userId") Long userId);
}
