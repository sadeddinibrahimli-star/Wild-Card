package com.wildcard.users;

import com.wildcard.common.BusinessException;
import com.wildcard.common.NotFoundException;
import com.wildcard.common.enums.AccountStatus;
import com.wildcard.users.enums.Role;
import com.wildcard.common.SecurityUtils;
import com.wildcard.common.PageResponse;
import com.wildcard.social.FollowRepository;
import com.wildcard.gamification.LevelCurve;
import com.wildcard.users.dto.DiscoverUserResponse;
import com.wildcard.gamification.Rarity;
import com.wildcard.gamification.TitleRule;
import com.wildcard.users.dto.UserCardResponse;
import com.wildcard.users.dto.UpdateProfileRequest;
import com.wildcard.users.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Transactional(readOnly = true)
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long id) {
        return toResponse(getEntityById(id));
    }
    /**
     * PUT /users/me — bio/avatar + username/email/parol dəyişikliyi.
     * Null olan sahə dəyişmir; parol üçün currentPassword tələb olunur.
     */
    @Transactional
    public UserProfileResponse updateProfile(Long id, UpdateProfileRequest request) {
        User user = getEntityById(id);
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new BusinessException("Account is suspended");
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // istifadəçi adı
        if (request.getUsername() != null) {
            String username = request.getUsername().trim();
            if (username.length() < 3) {
                throw new BusinessException("Username must be at least 3 characters");
            }
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                throw new BusinessException("Username is already taken");
            }
            user.setUsername(username);
        }

        // e-poçt
        if (request.getEmail() != null) {
            String email = request.getEmail().toLowerCase().trim();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new BusinessException("Email is already registered");
            }
            user.setEmail(email);
        }

        // yeni parol — yalnız cari parol düzgündürsə
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BusinessException("Current password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        UserProfileResponse response = toResponse(userRepository.save(user));
        response.setEmail(user.getEmail());
        return response;
    }

    /** GET /users/me — email yalnız öz profil cavabına əlavə olunur (publik profil sızdırmır). */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long id) {
        User user = getEntityById(id);
        UserProfileResponse response = toResponse(user);
        response.setEmail(user.getEmail());
        return response;
    }
    @Transactional(readOnly = true)
    public UserProfileResponse toResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() == null ? null : user.getRole().name())
                .accountStatus(user.getAccountStatus() == null ? null : user.getAccountStatus().name())
                .createdAt(user.getCreatedAt())
                .ani(user.getAni())
                .gam(user.getGam())
                .mus(user.getMus())
                .cha(user.getCha())
                .totalXp(user.getTotalXp())
                .level(user.getLevel())
                .rarity(user.getRarity())
                .title(user.getTitle())
                .currentStreak(user.getCurrentStreak())
                .longestStreak(user.getLongestStreak())
                .onboarded(user.isOnboarded())
                .lastActiveAt(user.getLastActiveAt())
                .build();
    }

    /** GET /users/{id}/card */
    @Transactional(readOnly = true)
    public UserCardResponse card(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return UserCardResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .title(user.getTitle())
                .flavorText(TitleRule.flavor(user.getAni(), user.getGam(), user.getMus(), user.getCha()))
                .rarity(user.getRarity())
                .level(user.getLevel())
                .ani(user.getAni())
                .gam(user.getGam())
                .mus(user.getMus())
                .cha(user.getCha())
                .totalXp(user.getTotalXp())
                .xpIntoLevel(LevelCurve.xpIntoLevel(user.getTotalXp()))
                .xpForNext(LevelCurve.xpForNext(user.getTotalXp()))
                .currentStreak(user.getCurrentStreak())
                .longestStreak(user.getLongestStreak())
                .build();
    }

    /** Public directory used by the Discover tab. */
    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> listUsers(String search, Pageable pageable) {
        User current = SecurityUtils.getCurrentUser();

        Page<User> page = (search == null || search.isBlank())
                ? userRepository.findAllByOrderByCreatedAtDesc(pageable)
                : userRepository.findByUsernameContainingIgnoreCaseOrderByCreatedAtDesc(search.trim(), pageable);

        List<Long> followingIds = followRepository.findFollowingIdsByFollowerId(current.getId());

        return PageResponse.from(page.map(u -> toResponse(u).toBuilder()
                .isFollowing(followingIds.contains(u.getId()))
                .build()));
    }

    /**
     * Discover: follow etmədiyimiz, aktiv istifadəçilər.
     * "reason" frontend ilə göstərilən kiçik izahdır.
     */
    @Transactional(readOnly = true)
    public PageResponse<DiscoverUserResponse> discover(Pageable pageable) {
        User current = SecurityUtils.getCurrentUser();
        List<Long> followingIds = followRepository.findFollowingIdsByFollowerId(current.getId());
        List<Long> exclude = new java.util.ArrayList<>(followingIds);
        exclude.add(current.getId());

        Page<User> page = userRepository.findDiscoverCandidates(current.getId(), exclude, pageable);

        return PageResponse.from(page.map(u -> DiscoverUserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .avatarUrl(u.getAvatarUrl())
                .bio(u.getBio())
                .level(LevelCurve.levelFor(u.getTotalXp()))
                .rarity(Rarity.fromLevel(LevelCurve.levelFor(u.getTotalXp())).name())
                .title(u.getTitle())
                .currentStreak(u.getCurrentStreak())
                .totalXp(u.getTotalXp())
                .lastActiveAt(u.getLastActiveAt())
                .reason(reasonFor(u))
                .isFollowing(false)
                .build()));
    }

    private String reasonFor(User u) {
        if (u.getCurrentStreak() >= 7) {
            return u.getCurrentStreak() + " day streak";
        }
        if (u.getLongestStreak() > u.getCurrentStreak() && u.getLongestStreak() >= 14) {
            return "Best streak " + u.getLongestStreak();
        }
        if (u.getLastActiveAt() != null) {
            return "Active recently";
        }
        return "New around here";
    }

    /**
     * Admin axtarisi: söz, hesab statusu və rola görə süzəcək.
     * Boş dəyər = həmin meyara görə süzməyən.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> adminSearch(String search,
                                                        AccountStatus status,
                                                        Role role,
                                                        Pageable pageable) {
        String q = search == null ? "" : search.trim();

        return PageResponse.from(
                userRepository.adminSearch(q, status, role, pageable).map(this::toResponse));
    }
}
