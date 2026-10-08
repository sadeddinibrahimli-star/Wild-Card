package com.wildcard.watchlist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    Optional<WatchlistItem> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTitleIgnoreCase(Long userId, String title);

    Page<WatchlistItem> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<WatchlistItem> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, WatchStatus status,
                                                                  Pageable pageable);

    long countByUserIdAndStatus(Long userId, WatchStatus status);

    long countByUserId(Long userId);

    long countByUserIdAndRatingIsNotNull(Long userId);

    @Query("select coalesce(avg(w.rating), 0) from WatchlistItem w " +
           "where w.user.id = :userId and w.rating is not null")
    double averageRating(@Param("userId") Long userId);


    /** review/notes yazilmis item sayi (LORE MASTER). */
    @Query("select count(w) from WatchlistItem w where w.user.id = :userId " +
           "and w.notes is not null and trim(w.notes) <> ''")
    long countReviewed(@Param("userId") Long userId);
}
