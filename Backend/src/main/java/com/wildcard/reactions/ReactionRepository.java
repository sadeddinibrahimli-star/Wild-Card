package com.wildcard.reactions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByPostIdAndUserId(Long postId, Long userId);

    @Query("""
            select r.type, count(r)
            from Reaction r
            where r.post.id = :postId
            group by r.type
            """)
    List<Object[]> countByTypeForPost(@Param("postId") Long postId);

    long countByPostId(Long postId);

    long countByUserId(Long userId);

    /** Reactions left on this user's posts. */
    @Query("select count(r) from Reaction r where r.post.author.id = :authorId")
    long countReceivedByAuthorId(@Param("authorId") Long authorId);
}
