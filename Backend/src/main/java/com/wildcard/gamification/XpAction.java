package com.wildcard.gamification;

import lombok.Getter;

@Getter
public enum XpAction {

    POST(20, null),
    COMMENT(5, XpCategory.CHA),
    REACTION_RECEIVED(3, XpCategory.CHA),
    WATCHLIST_UPDATE(10, XpCategory.ANI),
    MUSIC_ARC_UPDATE(10, XpCategory.MUS),
    MUSIC_CHECK_IN(5, XpCategory.MUS),
    WATCHLIST_COMPLETED(10, XpCategory.ANI),
    FOLLOWER_GAINED(2, XpCategory.CHA),
    ACHIEVEMENT_BONUS(0, XpCategory.CHA);

    private final int defaultValue;
    private final XpCategory defaultCategory;

    XpAction(int defaultValue, XpCategory defaultCategory) {
        this.defaultValue = defaultValue;
        this.defaultCategory = defaultCategory;
    }
}
