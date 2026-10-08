package com.wildcard.achievements;

import com.wildcard.users.User;

public record AchievementUnlockedEvent(User user, String code, int bonusXp) {
}
