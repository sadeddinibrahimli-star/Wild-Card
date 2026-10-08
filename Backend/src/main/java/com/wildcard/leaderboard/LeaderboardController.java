package com.wildcard.leaderboard;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.PageResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.leaderboard.dto.LeaderboardRowResponse;
import com.wildcard.leaderboard.dto.WeeklyPositionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/week")
    public ApiResponse<PageResponse<LeaderboardRowResponse>> week(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(leaderboardService.page(PageRequest.of(page, size)));
    }

    @GetMapping("/top")
    public ApiResponse<List<LeaderboardRowResponse>> top(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(leaderboardService.top(limit));
    }

    @GetMapping("/me")
    public ApiResponse<WeeklyPositionResponse> myPosition() {
        return ApiResponse.success(leaderboardService.myPosition(SecurityUtils.getCurrentUser()));
    }
}
