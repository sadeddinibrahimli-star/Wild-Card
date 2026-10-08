package com.wildcard.achievements;

import com.wildcard.achievements.dto.AchievementResponse;
import com.wildcard.comments.CommentRepository;
import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpCategory;
import com.wildcard.gamification.XpService;
import com.wildcard.music.MusicArcRepository;
import com.wildcard.notifications.NotificationService;
import com.wildcard.notifications.NotificationType;
import com.wildcard.posts.PostRepository;
import com.wildcard.reactions.ReactionRepository;
import com.wildcard.social.FollowRepository;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import com.wildcard.watchlist.WatchStatus;
import com.wildcard.watchlist.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import com.wildcard.gamification.XpGrantedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final UserRepository userRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final WatchlistRepository watchlistRepository;
    private final ReactionRepository reactionRepository;
    private final MusicArcRepository musicArcRepository;
    private final NotificationService notificationService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<AchievementResponse> forUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return allLocked(null);
        }

        Set<String> unlocked = new HashSet<>();
        Map<String, Instant> unlockedAt = new HashMap<>();
        for (UserAchievement row : userAchievementRepository.findByUserId(userId)) {
            unlocked.add(row.getCode());
            unlockedAt.put(row.getCode(), row.getUnlockedAt());
        }

        long posts = postRepository.countByAuthorIdAndDeletedFalseAndHiddenByModerationFalse(userId);
        long comments = commentRepository.countByAuthorIdAndDeletedFalse(userId);
        long followers = followRepository.countByFollowingId(userId);
        long completed = watchlistRepository.countByUserIdAndStatus(userId, WatchStatus.COMPLETED);
        long reviews = watchlistRepository.countReviewed(userId);
        long reactions = reactionRepository.countReceivedByAuthorId(userId);
        long bestPost = postRepository.maxReactionCountOnAuthorPost(userId);
        long nightOwl = postRepository.countNightOwlPosts(userId);
        int level = user.getLevel();
        long restDays = daysSinceLastActive(user);
        int musicStreak = musicStreakDays(userId);

        List<AchievementResponse> out = new ArrayList<>();
        for (AchievementDefinition def : AchievementDefinition.values()) {
            long current = switch (def) {
                case FIRST_BLOOD -> posts;
                case SIDE_QUEST -> comments;
                case TOUCH_GRASS -> restDays;
                case SOCIAL_BUTTERFLY -> followers;
                case BINGE_WATCHER -> completed;
                case ON_REPEAT -> musicStreak;
                case LORE_MASTER -> reviews;
                case CRITICAL_HIT -> reactions;
                case WHO_LET_YOU_COOK -> bestPost;
                case NIGHT_OWL -> nightOwl;
                case MAIN_CHARACTER -> level;
                case LEGEND -> level;
            };
            long target = switch (def) {
                case TOUCH_GRASS -> 3;
                case SOCIAL_BUTTERFLY -> 50;
                case BINGE_WATCHER -> 10;
                case ON_REPEAT -> 30;
                case LORE_MASTER -> 10;
                case CRITICAL_HIT -> 100;
                case WHO_LET_YOU_COOK -> 50;
                case NIGHT_OWL -> 1;
                case MAIN_CHARACTER -> 20;
                case LEGEND -> 30;
                default -> 1;
            };

            boolean isUnlocked = unlocked.contains(def.displayName());
            out.add(AchievementResponse.builder()
                    .code(def.displayName())
                    .name(def.displayName())
                    .title(def.displayName())
                    .description(def.description())
                    .rarity(def.rarity().name())
                    .icon(def.icon())
                    .current((int) Math.min(current, target))
                    .max((int) target)
                    .target((int) target)
                    .unlocked(isUnlocked)
                    .unlockedAt(isUnlocked ? unlockedAt.get(def.displayName()) : null)
                    .comingSoon(false)
                    .available(def.available())
                    .build());
        }
        return out;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void refresh(User user) {
        Long id = user.getId();

        long posts = postRepository.countByAuthorIdAndDeletedFalseAndHiddenByModerationFalse(id);
        long comments = commentRepository.countByAuthorIdAndDeletedFalse(id);
        long followers = followRepository.countByFollowingId(id);
        long completed = watchlistRepository.countByUserIdAndStatus(id, WatchStatus.COMPLETED);
        long reviews = watchlistRepository.countReviewed(id);
        long reactions = reactionRepository.countReceivedByAuthorId(id);
        long bestPost = postRepository.maxReactionCountOnAuthorPost(id);
        long nightOwl = postRepository.countNightOwlPosts(id);
        int level = user.getLevel();
        long restDays = daysSinceLastActive(user);
        int musicStreak = musicStreakDays(id);

        for (AchievementDefinition def : AchievementDefinition.values()) {
            boolean met = switch (def) {
                case FIRST_BLOOD -> posts >= 1;
                case SIDE_QUEST -> comments >= 1;
                case TOUCH_GRASS -> restDays >= 3;
                case SOCIAL_BUTTERFLY -> followers >= 50;
                case BINGE_WATCHER -> completed >= 10;
                case ON_REPEAT -> musicStreak >= 30;
                case LORE_MASTER -> reviews >= 10;
                case CRITICAL_HIT -> reactions >= 100;
                case WHO_LET_YOU_COOK -> bestPost >= 50;
                case NIGHT_OWL -> nightOwl >= 1;
                case MAIN_CHARACTER -> level >= 20;
                case LEGEND -> level >= 30;
            };

            if (!met || userAchievementRepository.existsByUserIdAndCode(id, def.displayName())) {
                continue;
            }

            userAchievementRepository.save(UserAchievement.builder()
                    .user(user)
                    .code(def.displayName())
                    .unlockedAt(Instant.now())
                    .build());

            notificationService.notify(user, NotificationType.ACHIEVEMENT,
                    def.displayName() + " unlocked (+" + def.rarity().bonusXp() + " XP)", null);

            eventPublisher.publishEvent(
                    new AchievementUnlockedEvent(user, def.displayName(), def.rarity().bonusXp()));
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onXpGranted(XpGrantedEvent event) {
        User user = event.user();
        refresh(user);

        if (event.leveledUp()) {
            notificationService.notify(user, NotificationType.LEVEL_UP,
                    "Level " + event.levelBefore() + " → " + event.levelAfter()
                            + " · " + user.getTitle(), null);
        }
    }

    private long daysSinceLastActive(User user) {
        if (user.getLastActiveAt() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(
                LocalDate.ofInstant(user.getLastActiveAt(), java.time.ZoneOffset.UTC),
                LocalDate.now());
    }

    private int musicStreakDays(Long userId) {
        List<java.time.LocalDateTime> arcs = musicArcRepository.startDatesFor(userId);
        if (arcs == null || arcs.isEmpty()) {
            return 0;
        }
        List<LocalDate> days = new ArrayList<>();
        for (java.time.LocalDateTime startedAt : arcs) {
            if (startedAt != null) {
                days.add(startedAt.toLocalDate());
            }
        }
        if (days.isEmpty()) {
            return 0;
        }
        days.sort((a, b) -> b.compareTo(a));

        Set<LocalDate> unique = new HashSet<>(days);
        int streak = 1;
        LocalDate cursor = unique.stream().sorted((a, b) -> b.compareTo(a)).findFirst().orElseThrow();
        for (LocalDate d : unique) {
            if (d.isBefore(cursor)) {
                continue;
            }
            if (d.equals(cursor) || d.equals(cursor.minusDays(1))) {
                if (d.equals(cursor)) {
                    continue;
                }
                streak++;
                cursor = d;
            }
        }
        return streak;
    }

    private List<AchievementResponse> allLocked(User user) {
        List<AchievementResponse> out = new ArrayList<>();
        for (AchievementDefinition def : AchievementDefinition.values()) {
            out.add(AchievementResponse.builder()
                    .code(def.displayName())
                    .name(def.displayName())
                    .title(def.displayName())
                    .description(def.description())
                    .rarity(def.rarity().name())
                    .icon(def.icon())
                    .current(0).max(1).target(1)
                    .unlocked(false).comingSoon(false).available(def.available())
                    .build());
        }
        return out;
    }
}