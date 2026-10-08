package com.wildcard.posts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByIdAndDeletedFalseAndHiddenByModerationFalse(Long id);

    Page<Post> findByDeletedFalseAndHiddenByModerationFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByCategoryAndDeletedFalseAndHiddenByModerationFalseOrderByCreatedAtDesc(Category category, Pageable pageable);

    Page<Post> findByAuthorIdAndDeletedFalseAndHiddenByModerationFalseOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    long countByDeletedFalseAndHiddenByModerationFalse();

    long countByDeletedFalseAndHiddenByModerationFalseAndCreatedAtAfter(LocalDateTime since);

    long countByAuthorIdAndDeletedFalseAndHiddenByModerationFalse(Long authorId);

    @Query("""
            select p from Post p
            where p.deleted = false and p.hiddenByModeration = false
              and (p.author.id = :userId or p.author.id in :followingIds)
            order by p.createdAt desc
            """)
    Page<Post> feedFor(@Param("userId") Long userId,
                       @Param("followingIds") List<Long> followingIds,
                       Pageable pageable);

    /** category və onlyFollowing filtrləri ilə eyni feed. */
    @Query("""
            select p from Post p
            where p.deleted = false and p.hiddenByModeration = false
              and (p.author.id = :userId or p.author.id in :followingIds)
              and (:category is null or p.category = :category)
              and (:onlyFollowing = false or p.author.id <> :userId)
            order by p.createdAt desc
            """)
    Page<Post> feedForFiltered(@Param("userId") Long userId,
                               @Param("followingIds") List<Long> followingIds,
                               @Param("category") Category category,
                               @Param("onlyFollowing") boolean onlyFollowing,
                               Pageable pageable);

    @Query("""
            select p from Post p
            where p.deleted = false and p.hiddenByModeration = false
              and (p.author.id = :userId or p.author.id in :followingIds)
            order by (
                (select count(c) from Comment c where c.post.id = p.id and c.deleted = false)
              + (select count(r) from Reaction r where r.post.id = p.id)
            ) desc, p.createdAt desc
            """)
    Page<Post> feedForByRelevance(@Param("userId") Long userId,
                                  @Param("followingIds") List<Long> followingIds,
                                  Pageable pageable);

    /**
     * ANA SƏİFƏ FEED-i: istifadəçinin mövzu çəkisinə görə sıralanır.
     *
     * Reytinq: mövzu affinity (bəyənilmiş mövzular öndə) -> paylaşım (reaction+comment)
     * -> yeni olmaq.
     *
     * id : bəyənilmiş mövzular
     */
    @Query("""
            select p from Post p
            left join p.topics pt
            left join TopicAffinity ta on ta.user.id = :userId and ta.topic.id = pt.id
            where p.deleted = false and p.hiddenByModeration = false
              and (pt.id in :likedTopicIds
                   or p.author.id in :followingIds
                   or p.author.id = :userId)
            group by p.id
            order by p.createdAt desc,
                     coalesce(sum(ta.score), 0) desc,
                     (select count(r) from Reaction r where r.post.id = p.id) desc
            """)
    Page<Post> homeFeed(@Param("userId") Long userId,
                        @Param("followingIds") List<Long> followingIds,
                        @Param("likedTopicIds") List<Long> likedTopicIds,
                        Pageable pageable);

    /**
     * Köhnə sıralama: əvvəlcə affinity, sonra reaksiya, en son tarix.
     * Doc 4.3 "sorted by recency OR relevance" - istifadəçi seçə bilir.
     */
    @Query("""
            select p from Post p
            left join p.topics pt
            left join TopicAffinity ta on ta.user.id = :userId and ta.topic.id = pt.id
            where p.deleted = false and p.hiddenByModeration = false
              and (pt.id in :likedTopicIds
                   or p.author.id in :followingIds
                   or p.author.id = :userId)
            group by p.id
            order by coalesce(sum(ta.score), 0) desc,
                     (select count(r) from Reaction r where r.post.id = p.id) desc,
                     p.createdAt desc
            """)
    Page<Post> homeFeedByRelevance(@Param("userId") Long userId,
                                  @Param("followingIds") List<Long> followingIds,
                                  @Param("likedTopicIds") List<Long> likedTopicIds,
                                  Pageable pageable);

    /**
     * Şəxsi feed az post göstərəndə (yeni istifadəçi heç kəsi izləmir,
     * heç bir mövzu seçməyib) boşluq doldurulur: platformadakı ən yeni postlar.
     */
    @Query("""
            select p from Post p
            where p.deleted = false and p.hiddenByModeration = false
              and p.id not in :excludeIds
            order by p.createdAt desc
            """)
    Page<Post> findRecentExcluding(@Param("excludeIds") List<Long> excludeIds, Pageable pageable);

    @Query("select function('format', p.createdAt, 'yyyy-MM-dd') as d, count(p) as c " +
           "from Post p where p.deleted = false and p.hiddenByModeration = false and p.createdAt >= :since " +
           "group by function('format', p.createdAt, 'yyyy-MM-dd') order by d")
    List<Object[]> countPerDaySince(@Param("since") LocalDateTime since);


    /** Bir istifadeçinin postlarindaki en yüksek reaksiya sayı (WHO LET YOU COOK). */
    @Query("select coalesce(max((select count(r) from Reaction r where r.post.id = p.id)), 0) " +
           "from Post p where p.author.id = :authorId and p.deleted = false and p.hiddenByModeration = false")
    long maxReactionCountOnAuthorPost(@Param("authorId") Long authorId);

    /** Gece 03:00-03:59 arasinda yazilmis post sayi (NIGHT OWL). */
    @Query("select count(p) from Post p where p.author.id = :authorId and p.deleted = false and p.hiddenByModeration = false " +
           "and cast(hour(p.createdAt) as integer) = 3")
    long countNightOwlPosts(@Param("authorId") Long authorId);
}