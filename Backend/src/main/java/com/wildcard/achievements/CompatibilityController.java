package com.wildcard.achievements;

import com.wildcard.achievements.dto.AchievementResponse;
import com.wildcard.achievements.dto.CompatibilityResponse;
import com.wildcard.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class CompatibilityController {

    private final CompatibilityService compatibilityService;
    private final AchievementService achievementService;

    @GetMapping("/{id}/compatibility/{otherId}")
    public ApiResponse<CompatibilityResponse> compatibility(
            @PathVariable Long id,
            @PathVariable Long otherId) {

        return ApiResponse.success(compatibilityService.between(id, otherId));
    }

    @GetMapping("/{id}/achievements")
    public ApiResponse<List<AchievementResponse>> achievements(@PathVariable Long id) {
        return ApiResponse.success(achievementService.forUser(id));
    }
}
