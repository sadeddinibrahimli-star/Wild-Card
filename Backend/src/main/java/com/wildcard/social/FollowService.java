package com.wildcard.social;

import com.wildcard.common.BusinessException;
import com.wildcard.common.NotFoundException;
import com.wildcard.common.PageResponse;
import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpCategory;
import com.wildcard.gamification.XpService;
import com.wildcard.notifications.NotificationService;
import com.wildcard.notifications.NotificationType;
import com.wildcard.social.dto.FollowResponse;
import com.wildcard.users.UserService;
import com.wildcard.users.dto.UserProfileResponse;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final XpService xpService;
    private final UserService userService;

    @Transactional
    public FollowResponse follow(User current, Long targetId) {
        if (current.getId().equals(targetId)) {
            throw new BusinessException("You cannot follow yourself");
        }
        if (followRepository.existsByFollowerIdAndFollowingId(current.getId(), targetId)) {
            throw new BusinessException("You already follow this user");
        }

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException("User not found: " + targetId));

        Follow saved = followRepository.save(
                Follow.builder().follower(current).following(target).build());

        notificationService.notify(target, NotificationType.FOLLOW,
                current.getUsername() + " started following you", null);

        xpService.grant(target, XpAction.FOLLOWER_GAINED, XpCategory.CHA);

        return toResponse(saved);
    }

    @Transactional
    public void unfollow(User current, Long targetId) {
        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(current.getId(), targetId)
                .orElseThrow(() -> new BusinessException("You do not follow this user"));

        followRepository.delete(follow);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long targetId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, targetId);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> following(Long userId, Pageable pageable) {
        return PageResponse.from(
                followRepository.findAllByFollowerId(userId, pageable)
                        .map(Follow::getFollowing)
                        .map(userService::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> followers(Long userId, Pageable pageable) {
        return PageResponse.from(
                followRepository.findAllByFollowingId(userId, pageable)
                        .map(Follow::getFollower)
                        .map(userService::toResponse));
    }

    @Transactional(readOnly = true)
    public List<Long> followingIds(Long userId) {
        return followRepository.findFollowingIdsByFollowerId(userId);
    }

    private FollowResponse toResponse(Follow follow) {
        return FollowResponse.builder()
                .id(follow.getId())
                .followerId(follow.getFollower().getId())
                .followerUsername(follow.getFollower().getUsername())
                .followingId(follow.getFollowing().getId())
                .followingUsername(follow.getFollowing().getUsername())
                .createdAt(follow.getCreatedAt())
                .build();
    }
}
