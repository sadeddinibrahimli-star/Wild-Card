package com.wildcard.users;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.music.dto.UserMusicArcResponse;
import com.wildcard.users.dto.DiscoverUserResponse;
import com.wildcard.users.dto.UpdateProfileRequest;
import com.wildcard.users.dto.UserCardResponse;
import com.wildcard.users.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.wildcard.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final com.wildcard.music.UserMusicArcService userMusicArcService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        // the email only appears in the user's own response (a public profile does not leak it)
        return ApiResponse.success(userService.getMyProfile(SecurityUtils.getCurrentUser().getId()));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserProfileResponse>> directory(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {

        return ApiResponse.success(
                userService.listUsers(search, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}/card")
    public ApiResponse<UserCardResponse> card(@PathVariable Long id) {
        return ApiResponse.success(userService.card(id));
    }

    @GetMapping("/discover")
    public ApiResponse<PageResponse<DiscoverUserResponse>> discover(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {

        return ApiResponse.success(userService.discover(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}/music-arc")
    public ApiResponse<UserMusicArcResponse> musicArc(@PathVariable Long id) {
        return ApiResponse.success(userMusicArcService.of(id));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserProfileResponse> getProfile(@PathVariable Long id) {
        return ApiResponse.success(userService.getProfile(id));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profile updated",
                userService.updateProfile(SecurityUtils.getCurrentUser().getId(), request));
    }
}