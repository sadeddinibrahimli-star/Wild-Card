package com.wildcard.moderation;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.PageResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.moderation.dto.CreateReportRequest;
import com.wildcard.moderation.dto.CreateWarningRequest;
import com.wildcard.moderation.dto.ModerationActionResponse;
import com.wildcard.moderation.dto.ReportResponse;
import com.wildcard.moderation.dto.ResolveReportRequest;
import com.wildcard.moderation.dto.UpdateAccountStatusRequest;
import com.wildcard.moderation.dto.UserWarningResponse;
import com.wildcard.users.UserService;
import com.wildcard.users.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @PostMapping("/reports")
    public ApiResponse<ReportResponse> create(@Valid @RequestBody CreateReportRequest request) {
        return ApiResponse.success("Report submitted",
                reportService.create(SecurityUtils.getCurrentUser(), request));
    }

    @GetMapping("/moderation/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ApiResponse<PageResponse<ReportResponse>> list(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reportService.list(status, PageRequest.of(page, size)));
    }

    @PatchMapping("/moderation/reports/{reportId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ApiResponse<ReportResponse> resolve(@PathVariable Long reportId,
                                                @Valid @RequestBody ResolveReportRequest request) {
        return ApiResponse.success("Report updated",
                reportService.resolve(SecurityUtils.getCurrentUser(), reportId, request));
    }

    @PatchMapping("/moderation/users/{userId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ApiResponse<UserProfileResponse> changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateAccountStatusRequest request) {

        var updated = reportService.changeAccountStatus(
                SecurityUtils.getCurrentUser(), userId, request.getStatus());

        return ApiResponse.success("Account status updated", userService.toResponse(updated));
    }

    @PostMapping("/moderation/users/{userId}/warn")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ApiResponse<UserWarningResponse> warn(@PathVariable Long userId,
                                                 @Valid @RequestBody CreateWarningRequest request) {
        return ApiResponse.success("Warning issued",
                reportService.warn(SecurityUtils.getCurrentUser(), userId, request.getReason()));
    }

    @GetMapping("/moderation/users/{userId}/warnings")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ApiResponse<PageResponse<UserWarningResponse>> warnings(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reportService.warningsFor(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/moderation/users/{userId}/flagged-content")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ApiResponse<PageResponse<ReportResponse>> flaggedContent(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reportService.flaggedContentFor(userId, PageRequest.of(page, size)));
    }

    // ---------- məzmunu gizlətmə / silmə ----------

    @PatchMapping("/moderation/posts/{postId}/hide")
    public ApiResponse<ModerationActionResponse> hidePost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "true") boolean hide) {

        return ApiResponse.success(reportService.hidePost(postId, hide, currentUserId()));
    }

    @DeleteMapping("/moderation/posts/{postId}")
    public ApiResponse<ModerationActionResponse> removePost(@PathVariable Long postId) {
        return ApiResponse.success(reportService.removePost(postId, currentUserId()));
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUser().getId();
    }
}
