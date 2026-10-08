package com.wildcard.gamification;

import com.wildcard.users.User;

/**
 * XP yazıldıqdan sonra yayılır. AchievementService bunu dinləyir;
 * beləliklə XpService -> AchievementService asılılığı olmur və
 * dairəvi dependency yaranmır.
 */
public record XpGrantedEvent(User user, int levelBefore, int levelAfter) {

    public boolean leveledUp() {
        return levelAfter > levelBefore;
    }
}