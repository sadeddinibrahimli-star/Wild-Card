package com.wildcard.admin;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.NotFoundException;
import com.wildcard.common.PageResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import com.wildcard.users.UserService;
import com.wildcard.users.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final AdminUserService adminUserService;
    private final AdminStatsService adminStatsService;
    private final AdminStatsCache adminStatsCache;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(adminStatsCache.get());
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserProfileResponse>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.wildcard.common.enums.AccountStatus status,
            @RequestParam(required = false) com.wildcard.users.enums.Role role) {

        var spec = org.springframework.data.domain.PageRequest.of(page, size);
        var all = userService.adminSearch(search, status, role, spec);
        return ApiResponse.success(all);
    }

    /** Doc 4.1: "Create ... user accounts" */
    @PostMapping("/users")
    public ApiResponse<UserProfileResponse> createUser(
            @jakarta.validation.Valid @RequestBody com.wildcard.admin.dto.CreateUserRequest request) {
        return ApiResponse.success("Account created", adminUserService.create(request));
    }

    /** Doc 4.1: "update ... user accounts" */
    @PatchMapping("/users/{userId}")
    public ApiResponse<UserProfileResponse> updateUser(
            @PathVariable Long userId,
            @jakarta.validation.Valid @RequestBody com.wildcard.admin.dto.UpdateUserRequest request) {
        return ApiResponse.success("Account updated", adminUserService.update(userId, request));
    }

    /** Doc 4.1: "suspend, and reactivate" */
    @PatchMapping("/users/{userId}/status")
    public ApiResponse<UserProfileResponse> changeStatus(
            @PathVariable Long userId,
            @RequestParam com.wildcard.common.enums.AccountStatus status) {

        return ApiResponse.success("Account status updated: " + status,
                adminUserService.changeStatus(SecurityUtils.getCurrentUser().getId(), userId, status));
    }

    @PatchMapping("/users/{userId}/role")
    public ApiResponse<UserProfileResponse> changeRole(
            @PathVariable Long userId,
            @RequestParam com.wildcard.users.enums.Role role) {

        var request = new com.wildcard.admin.dto.UpdateUserRequest();
        request.setRole(role);
        return ApiResponse.success("Role updated", adminUserService.update(userId, request));
    }
}
