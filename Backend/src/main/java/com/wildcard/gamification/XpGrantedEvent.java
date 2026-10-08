package com.wildcard.gamification;

import com.wildcard.users.User;

/**
 * Published after XP has been written. AchievementService listens for it,
 * so there is no XpService -> AchievementService dependency and
 * no circular dependency.
 */
public record XpGrantedEvent(User user, int levelBefore, int levelAfter) {

    public boolean leveledUp() {
        return levelAfter > levelBefore;
    }
}