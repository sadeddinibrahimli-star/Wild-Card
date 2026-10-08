package com.wildcard.comments;

import com.wildcard.common.BusinessException;
import com.wildcard.common.ContentSanitizer;
import com.wildcard.common.NotFoundException;
import com.wildcard.common.PageResponse;
import com.wildcard.common.RateLimiter;
import com.wildcard.common.SecurityUtils;
import com.wildcard.comments.dto.CommentResponse;
import com.wildcard.comments.dto.CreateCommentRequest;
import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpService;
import com.wildcard.notifications.NotificationService;
import com.wildcard.notifications.NotificationType;
import com.wildcard.posts.Post;
import com.wildcard.posts.PostRepository;
import com.wildcard.users.enums.Role;
import com.wildcard.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final XpService xpService;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;

    @Transactional
    public CommentResponse create(User author, Long postId, CreateCommentRequest request) {
        SecurityUtils.requireCanPublish(author);
        rateLimiter.checkComments(author);

        Post post = postRepository.findByIdAndDeletedFalseAndHiddenByModerationFalse(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        Comment saved = commentRepository.save(Comment.builder()
                .post(post)
                .author(author)
                .body(ContentSanitizer.richText(request.getBody()))
                .build());

        xpService.grant(author, XpAction.COMMENT);

        if (!post.getAuthor().getId().equals(author.getId())) {
            notificationService.notify(post.getAuthor(), NotificationType.COMMENT,
                    author.getUsername() + " commented on your post", post.getId());
        }

        return toResponse(saved);
    }

    @Transactional
    public void softDelete(User current, Long commentId) {
        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));

        boolean isOwner = comment.getAuthor().getId().equals(current.getId());
        boolean isStaff = current.getRole() == Role.ADMIN || current.getRole() == Role.MODERATOR;

        if (!isOwner && !isStaff) {
            throw new BusinessException("You can only delete your own comments");
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> listFor(Long postId, Pageable pageable) {
        postRepository.findByIdAndDeletedFalseAndHiddenByModerationFalse(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        return PageResponse.from(
                commentRepository.findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId, pageable)
                        .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public long countActiveFor(Long postId) {
        return commentRepository.countByPostIdAndDeletedFalse(postId);
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthor().getId())
                .authorUsername(comment.getAuthor().getUsername())
                .authorAvatarUrl(comment.getAuthor().getAvatarUrl())
                .body(comment.getBody())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}