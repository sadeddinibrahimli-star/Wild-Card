package com.wildcard.gamification;

import lombok.Getter;

@Getter
public enum Rarity {

    COMMON(0),
    RARE(10),
    EPIC(25),
    LEGENDARY(50);

    private final int minLevel;

    Rarity(int minLevel) {
        this.minLevel = minLevel;
    }

    public static Rarity fromLevel(int level) {
        Rarity result = COMMON;
        for (Rarity rarity : values()) {
            if (level >= rarity.minLevel) {
                result = rarity;
            }
        }
        return result;
    }
}
