package com.wildcard.admin;

import com.wildcard.admin.dto.CreateUserRequest;
import com.wildcard.admin.dto.UpdateUserRequest;
import com.wildcard.common.BusinessException;
import com.wildcard.common.ContentSanitizer;
import com.wildcard.common.NotFoundException;
import com.wildcard.common.enums.AccountStatus;
import com.wildcard.gamification.CardService;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import com.wildcard.users.UserService;
import com.wildcard.users.dto.UserProfileResponse;
import com.wildcard.users.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account creation, editing, suspend/reactivate (doc 4.1 Administration).
 *
 * Rules:
 *  - an admin cannot suspend their own account (no self-lockout)
 *  - the role/status of the last active admin cannot be changed
 *  - email and username stay unique
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CardService cardService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserProfileResponse create(CreateUserRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String username = request.getUsername().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email is already registered");
        }
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Username is already taken");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRole(request.getRole() == null ? Role.USER : request.getRole());
        user.setLevel(1);

        return userService.toResponse(userRepository.save(user));
    }

    /** Null fields are left unchanged. */
    @Transactional
    public UserProfileResponse update(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (request.getUsername() != null) {
            String username = request.getUsername().trim();
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                throw new BusinessException("Username is already taken");
            }
            user.setUsername(username);
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().toLowerCase().trim();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new BusinessException("Email is already registered");
            }
            user.setEmail(email);
        }

        if (request.getBio() != null) {
            user.setBio(ContentSanitizer.text(request.getBio()));
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().isBlank() ? null : request.getAvatarUrl());
        }
        if (request.getOnboarded() != null) {
            user.setOnboarded(request.getOnboarded());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            guardLastAdmin(user, request.getRole(), user.getAccountStatus());
            user.setRole(request.getRole());
        }
        if (request.getAccountStatus() != null) {
            guardLastAdmin(user, user.getRole(), request.getAccountStatus());
            user.setAccountStatus(request.getAccountStatus());
        }

        User saved = userRepository.save(user);
        // stats (ani/gam/mus/cha) can be stale after a role change
        cardService.recompute(saved);
        return userService.toResponse(saved);
    }

    /** Suspend/reactivate. RESTRICTED only blocks creating posts. */
    @Transactional
    public UserProfileResponse changeStatus(Long actorId, Long userId, AccountStatus status) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (userId.equals(actorId) && status != AccountStatus.ACTIVE) {
            throw new BusinessException("You cannot restrict or suspend your own account");
        }
        if (status == target.getAccountStatus()) {
            return userService.toResponse(target);
        }

        guardLastAdmin(target, target.getRole(), status);
        target.setAccountStatus(status);
        User saved = userRepository.save(target);
        cardService.recompute(saved);
        return userService.toResponse(saved);
    }

    private void guardLastAdmin(User user, Role newRole, AccountStatus newStatus) {
        boolean losesAdmin = user.getRole() == Role.ADMIN
                && (newRole != Role.ADMIN || newStatus == AccountStatus.SUSPENDED);
        if (!losesAdmin) {
            return;
        }
        long activeAdmins = userRepository.findByRole(Role.ADMIN).stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                .count();
        if (activeAdmins <= 1) {
            throw new BusinessException("This is the last active admin - promote someone else first");
        }
    }
}