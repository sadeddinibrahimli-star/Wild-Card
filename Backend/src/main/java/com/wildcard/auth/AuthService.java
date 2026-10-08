package com.wildcard.auth;

import com.wildcard.auth.dto.AuthResponse;
import com.wildcard.auth.dto.LoginRequest;
import com.wildcard.auth.dto.RegisterUserRequest;
import com.wildcard.auth.dto.TokenRefreshRequest;
import com.wildcard.auth.dto.TokenRefreshResponse;
import com.wildcard.common.BusinessException;
import com.wildcard.common.enums.AccountStatus;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import com.wildcard.users.UserService;
import com.wildcard.users.enums.Role;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterUserRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username is already taken");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRole(Role.USER);
        user.setLevel(1);

        return toAuthResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Invalid email or password");
        }
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new BusinessException("Account is suspended");
        }

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String token = request.getRefreshToken();

        if (!isRefreshToken(token)) {
            throw new BusinessException("Provided token is not a refresh token");
        }

        User user = userRepository.findById(jwtService.extractUserId(token))
                .orElseThrow(() -> new BusinessException("User no longer exists"));

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new BusinessException("Account is suspended");
        }

        return TokenRefreshResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .accessTokenExpiresIn(jwtService.getAccessTokenSeconds())
                .build();
    }

    private boolean isRefreshToken(String token) {
        try {
            return jwtService.isOfType(token, "refresh");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException("Invalid or expired refresh token");
        }
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .accessTokenExpiresIn(jwtService.getAccessTokenSeconds())
                .user(userService.toResponse(user))
                .build();
    }
}