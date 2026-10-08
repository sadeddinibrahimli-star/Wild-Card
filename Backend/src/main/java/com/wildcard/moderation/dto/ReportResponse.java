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

    // ---- doc 4.2: "View and triage reported posts and comments" ----
    // Moderator baxanda SIZI GOREBILMELIDIR - sadece id deyil, məzmunun önizlənməsi.
    /** Poçt şikayət edilibsə: başlıq / mətn / şəkil / müəllif. */
    private String reportedPostTitle;
    private String reportedPostBody;
    private String reportedPostImageUrl;
    private Long reportedPostAuthorId;
    private String reportedPostAuthorUsername;
    /** Poçt artıq gizlədilmiş/silinmişdir? */
    private boolean reportedPostHidden;

    /** Komment şikayət edilibsə: mətn / müəllif. */
    private String reportedCommentBody;
    private Long reportedCommentAuthorId;
    private String reportedCommentAuthorUsername;

    /** Hansı növ şikayət: POST | COMMENT */
    private String targetType;
    private ReportStatus status;
    private String resolutionNote;
    private Long handledById;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
