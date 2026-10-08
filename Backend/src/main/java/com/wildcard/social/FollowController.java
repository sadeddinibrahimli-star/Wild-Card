package com.wildcard.social;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.PageResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.social.dto.FollowResponse;
import com.wildcard.users.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/social/follow/{userId}")
    public ApiResponse<FollowResponse> follow(@PathVariable Long userId) {
        return ApiResponse.success("Followed",
                followService.follow(SecurityUtils.getCurrentUser(), userId));
    }

    @DeleteMapping("/social/follow/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable Long userId) {
        followService.unfollow(SecurityUtils.getCurrentUser(), userId);
    }

    @GetMapping("/social/follow/{userId}/status")
    public ApiResponse<Boolean> status(@PathVariable Long userId) {
        return ApiResponse.success(
                followService.isFollowing(SecurityUtils.getCurrentUser().getId(), userId));
    }

    @GetMapping("/users/{userId}/following")
    public ApiResponse<PageResponse<UserProfileResponse>> following(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(followService.following(userId, pageable(page, size)));
    }

    @GetMapping("/users/{userId}/followers")
    public ApiResponse<PageResponse<UserProfileResponse>> followers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(followService.followers(userId, pageable(page, size)));
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}