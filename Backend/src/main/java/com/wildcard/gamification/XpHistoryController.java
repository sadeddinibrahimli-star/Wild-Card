package com.wildcard.gamification;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.PageResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.gamification.dto.XpLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Own XP history only (doc 10 - your own data).
 * XP is only ever written by XpService, so this endpoint is read-only.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class XpHistoryController {

    private final XpService xpService;

    @GetMapping("/me/xp-history")
    public ApiResponse<PageResponse<XpLogResponse>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(
                xpService.historyPage(SecurityUtils.getCurrentUserId(), PageRequest.of(page, size)));
    }
}
