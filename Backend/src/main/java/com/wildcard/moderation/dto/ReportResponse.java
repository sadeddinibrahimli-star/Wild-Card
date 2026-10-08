package com.wildcard.moderation.dto;

import com.wildcard.moderation.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private Long id;
    private Long reporterId;
    private String reporterUsername;
    private Long postId;
    private Long commentId;
    private String reason;

    // a moderator must SEE the content, not just the id - include a preview
    private String reportedPostTitle;
    private String reportedPostBody;
    private String reportedPostImageUrl;
    private Long reportedPostAuthorId;
    private String reportedPostAuthorUsername;
    /** true when the reported post is already hidden or deleted. */
    private boolean reportedPostHidden;

    private String reportedCommentBody;
    private Long reportedCommentAuthorId;
    private String reportedCommentAuthorUsername;

    /** POST | COMMENT */
    private String targetType;
    private ReportStatus status;
    private String resolutionNote;
    private Long handledById;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
