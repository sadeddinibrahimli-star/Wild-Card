package com.wildcard.auth;

import com.wildcard.auth.dto.AuthResponse;
import com.wildcard.auth.dto.LoginRequest;
import com.wildcard.auth.dto.RegisterUserRequest;
import com.wildcard.auth.dto.TokenRefreshRequest;
import com.wildcard.auth.dto.TokenRefreshResponse;
import com.wildcard.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.wildcard.auth.dto.ForgotPasswordRequest;
import com.wildcard.auth.dto.ResetPasswordRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ApiResponse.success("Registered", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Logged in", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }


    /**
     * Always returns the same response so the existence of a user
     * is never leaked.
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return ApiResponse.success("If that email exists we sent a reset link", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request);
        return ApiResponse.success("Password updated", null);
    }
}