package com.wildcard.comments;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdAndDeletedFalseOrderByCreatedAtAsc(Long postId, Pageable pageable);

    Optional<Comment> findByIdAndDeletedFalse(Long id);

    long countByPostIdAndDeletedFalse(Long postId);

    long countByAuthorIdAndDeletedFalse(Long authorId);

    long countById(Long id);
}
