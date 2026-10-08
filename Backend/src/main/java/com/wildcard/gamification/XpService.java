package com.wildcard.gamification;

import com.wildcard.achievements.AchievementService;
import com.wildcard.common.BusinessException;
import com.wildcard.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class XpService {

    private final XpLogRepository xpLogRepository;
    private final XpConfigRepository xpConfigRepository;
    private final CardService cardService;
    private final StreakService streakService;
    // circular dependency: AchievementService listens to events, while
    // @Lazy breaks the cycle: XpService computes first for TOUCH_GRASS.
    private final @org.springframework.context.annotation.Lazy AchievementService achievementService;

    private final ApplicationEventPublisher eventPublisher;

    /** Daily maximum per action type - anti-spam. */
    private static final Map<XpAction, Integer> DAILY_LIMITS = Map.of(
            XpAction.POST, 10,
            XpAction.COMMENT, 30,
            XpAction.REACTION_RECEIVED, 100,
            XpAction.WATCHLIST_UPDATE, 20,
            XpAction.WATCHLIST_COMPLETED, 20,
            XpAction.MUSIC_ARC_UPDATE, 10,
            XpAction.FOLLOWER_GAINED, 50);

    private volatile Map<XpAction, Integer> values = new EnumMap<>(XpAction.class);

    public int valueOf(XpAction action) {
        Integer value = values.get(action);
        return value != null ? value : action.getDefaultValue();
    }

    @Transactional
    public void seedConfigIfEmpty() {
        for (XpAction action : XpAction.values()) {
            if (xpConfigRepository.findByAction(action).isEmpty()) {
                xpConfigRepository.save(XpConfig.builder()
                        .action(action)
                        .value(action.getDefaultValue())
                        .build());
            }
        }
        reloadConfig();
    }

    @Transactional(readOnly = true)
    public void reloadConfig() {
        Map<XpAction, Integer> loaded = new EnumMap<>(XpAction.class);
        for (XpAction action : XpAction.values()) {
            loaded.put(action, xpConfigRepository.findByAction(action)
                    .map(XpConfig::getValue)
                    .orElse(action.getDefaultValue()));
        }
        this.values = loaded;
    }

    /** Grant a fixed amount (achievement bonus style). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void grant(User user, XpAction action, XpCategory category, int amount) {
        if (amount <= 0) {
            return;
        }
        writeLog(user, action, category, amount);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void grant(User user, XpAction action, XpCategory category) {
        int amount = valueOf(action);
        if (amount <= 0 || isDailyLimitReached(user, action)) {
            return;
        }
        writeLog(user, action, category, amount);
    }

    private void writeLog(User user, XpAction action, XpCategory category, int amount) {
        xpLogRepository.save(XpLog.builder()
                .user(user)
                .category(category)
                .action(action)
                .amount(amount)
                .build());

        int levelBefore = user.getLevel();
        cardService.recompute(user);

        // The streak is updated BEFORE the badges are calculated - TOUCH_GRASS
        // means "came back after 3 days", so the previous lastActiveAt is needed.
        // Calling recordActivity() after the calculation would set lastActiveAt = now
        // and this badge would never unlock.
        achievementService.refresh(user);

        streakService.recordActivity(user);

        eventPublisher.publishEvent(new XpGrantedEvent(user, levelBefore, user.getLevel()));
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void onAchievementUnlocked(com.wildcard.achievements.AchievementUnlockedEvent event) {
        grant(event.user(), XpAction.ACHIEVEMENT_BONUS, XpCategory.CHA, event.bonusXp());
    }

    private boolean isDailyLimitReached(User user, XpAction action) {
        Integer limit = DAILY_LIMITS.get(action);
        if (limit == null) {
            return false;
        }
        long today = xpLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                user.getId(), action, LocalDate.now().atStartOfDay());
        return today >= limit;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void grant(User user, XpAction action) {
        if (action.getDefaultCategory() == null) {
            throw new BusinessException("Action " + action + " needs an explicit category");
        }
        grant(user, action, action.getDefaultCategory());
    }

    @Transactional(readOnly = true)
    public List<XpLog> historyFor(Long userId) {
        return xpLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public com.wildcard.common.PageResponse<com.wildcard.gamification.dto.XpLogResponse> historyPage(
            Long userId, org.springframework.data.domain.Pageable pageable) {

        var page = xpLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return com.wildcard.common.PageResponse.from(page.map(l ->
                com.wildcard.gamification.dto.XpLogResponse.builder()
                        .id(l.getId())
                        .action(l.getAction())
                        .category(l.getCategory())
                        .amount(l.getAmount())
                        .createdAt(l.getCreatedAt())
                        .build()));
    }
}