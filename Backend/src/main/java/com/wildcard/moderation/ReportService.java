package com.wildcard.moderation;

import com.wildcard.common.BusinessException;
import com.wildcard.common.ContentSanitizer;
import com.wildcard.common.NotFoundException;
import com.wildcard.common.PageResponse;
import com.wildcard.common.enums.AccountStatus;
import com.wildcard.comments.CommentRepository;
import com.wildcard.moderation.dto.CreateReportRequest;
import com.wildcard.moderation.dto.ModerationActionResponse;
import com.wildcard.moderation.dto.ReportResponse;
import com.wildcard.moderation.dto.ResolveReportRequest;
import com.wildcard.moderation.dto.UserWarningResponse;
import com.wildcard.notifications.NotificationService;
import com.wildcard.notifications.NotificationType;
import com.wildcard.posts.Post;
import com.wildcard.posts.PostRepository;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserWarningRepository userWarningRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReportResponse create(User reporter, CreateReportRequest request) {
        if (request.getPostId() == null && request.getCommentId() == null) {
            throw new BusinessException("A report must target a post or a comment");
        }

        Post post = null;
        com.wildcard.comments.Comment comment = null;

        if (request.getPostId() != null) {
            post = postRepository.findByIdAndDeletedFalseAndHiddenByModerationFalse(request.getPostId())
                    .orElseThrow(() -> new NotFoundException("Post not found: " + request.getPostId()));
        }
        if (request.getCommentId() != null) {
            comment = commentRepository.findByIdAndDeletedFalse(request.getCommentId())
                    .orElseThrow(() -> new NotFoundException(
                            "Comment not found: " + request.getCommentId()));
        }

        Report saved = reportRepository.save(Report.builder()
                .reporter(reporter)
                .post(post)
                .comment(comment)
                .reason(ContentSanitizer.text(request.getReason()))
                .status(ReportStatus.PENDING)
                .build());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> list(ReportStatus status, Pageable pageable) {
        var page = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                : reportRepository.findByStatusOrderByCreatedAtAsc(status, pageable);

        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> flaggedContentFor(Long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        return PageResponse.from(
                reportRepository.findFlaggedContentForUser(userId, pageable).map(this::toResponse));
    }

    @Transactional
    public ReportResponse resolve(User moderator, Long reportId, ResolveReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found: " + reportId));

        report.setStatus(request.getStatus());
        report.setResolutionNote(ContentSanitizer.text(request.getResolutionNote()));
        report.setHandledBy(moderator);
        report.setHandledAt(LocalDateTime.now());

        if (request.isRemoveContent()) {
            if (report.getPost() != null) {
                report.getPost().setDeleted(true);
            }
            if (report.getComment() != null) {
                report.getComment().setDeleted(true);
            }
        }

        Report saved = reportRepository.save(report);

        notificationService.notify(saved.getReporter(), NotificationType.REPORT_RESOLVED,
                "Your report was " + saved.getStatus().name().toLowerCase(), null);

        return toResponse(saved);
    }

    @Transactional
    public UserWarningResponse warn(User moderator, Long userId, String reason) {
        if (moderator.getId().equals(userId)) {
            throw new BusinessException("You cannot warn yourself");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        UserWarning saved = userWarningRepository.save(UserWarning.builder()
                .user(target)
                .moderator(moderator)
                .reason(ContentSanitizer.text(reason))
                .build());

        notificationService.notify(target, NotificationType.SYSTEM,
                "You received a warning: " + saved.getReason(), null);

        return toWarningResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserWarningResponse> warningsFor(Long userId, Pageable pageable) {
        return PageResponse.from(
                userWarningRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                        .map(this::toWarningResponse));
    }

    @Transactional
    public User changeAccountStatus(User actor, Long userId, AccountStatus status) {
        if (actor.getId().equals(userId)) {
            throw new BusinessException("You cannot change your own account status");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        target.setAccountStatus(status);
        return userRepository.save(target);
    }

    @Transactional
    public User changeRole(User actor, Long userId, com.wildcard.users.enums.Role role) {
        if (actor.getId().equals(userId)) {
            throw new BusinessException("You cannot change your own role");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        target.setRole(role);
        return userRepository.save(target);
    }

    private ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterUsername(report.getReporter().getUsername())
                .postId(report.getPost() == null ? null : report.getPost().getId())
                .commentId(report.getComment() == null ? null : report.getComment().getId())
                .reason(report.getReason())
                .targetType(report.getPost() != null ? "POST" : "COMMENT")
                .reportedPostTitle(report.getPost() == null ? null : report.getPost().getTitle())
                .reportedPostBody(report.getPost() == null ? null : report.getPost().getBody())
                .reportedPostImageUrl(report.getPost() == null ? null : report.getPost().getImageUrl())
                .reportedPostAuthorId(report.getPost() == null ? null : report.getPost().getAuthor().getId())
                .reportedPostAuthorUsername(
                        report.getPost() == null ? null : report.getPost().getAuthor().getUsername())
                .reportedPostHidden(report.getPost() != null
                        && (report.getPost().isDeleted() || report.getPost().isHiddenByModeration()))
                .reportedCommentBody(report.getComment() == null ? null : report.getComment().getBody())
                .reportedCommentAuthorId(
                        report.getComment() == null ? null : report.getComment().getAuthor().getId())
                .reportedCommentAuthorUsername(
                        report.getComment() == null ? null : report.getComment().getAuthor().getUsername())
                .status(report.getStatus())
                .resolutionNote(report.getResolutionNote())
                .handledById(report.getHandledBy() == null ? null : report.getHandledBy().getId())
                .handledAt(report.getHandledAt())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private UserWarningResponse toWarningResponse(UserWarning warning) {
        return UserWarningResponse.builder()
                .id(warning.getId())
                .userId(warning.getUser().getId())
                .username(warning.getUser().getUsername())
                .moderatorId(warning.getModerator().getId())
                .moderatorUsername(warning.getModerator().getUsername())
                .reason(warning.getReason())
                .createdAt(warning.getCreatedAt())
                .build();
    }

    /**
     * Hide the content through moderation (soft).
     * The content stays in the database and can be restored.
     */
    @Transactional
    public ModerationActionResponse hidePost(Long postId, boolean hide, Long moderatorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        post.setHiddenByModeration(hide);
        postRepository.save(post);

        return ModerationActionResponse.builder()
                .action(hide ? "POST_HIDDEN" : "POST_RESTORED")
                .postId(postId)
                .hidden(hide)
                .message(hide ? "Post hidden from public feeds" : "Post restored")
                .build();
    }

    /** Delete the content (soft-delete). */
    @Transactional
    public ModerationActionResponse removePost(Long postId, Long moderatorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        post.setDeleted(true);
        post.setHiddenByModeration(true);
        postRepository.save(post);

        return ModerationActionResponse.builder()
                .action("POST_REMOVED")
                .postId(postId)
                .hidden(true)
                .message("Post removed")
                .build();
    }
}
