package com.wildcard.posts;

import com.wildcard.common.BusinessException;
import com.wildcard.gamification.XpCategory;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Category {

    ANIME(XpCategory.ANI),
    MUSIC(XpCategory.MUS),
    FILM(XpCategory.ANI),
    GAMING(XpCategory.GAM),
    DND(XpCategory.CHA);

    private final XpCategory primaryStat;

    Category(XpCategory primaryStat) {
        this.primaryStat = primaryStat;
    }

    public static Category from(String value) {
        return Arrays.stream(values())
                .filter(category -> category.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Unknown category: " + value + ". Allowed: " + Arrays.toString(values())));
    }
}