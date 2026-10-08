package com.wildcard.achievements;

/** 12 badge-in tərifi. Backend qaydası olmayanlar {@code available=false} qalır. */
public enum AchievementDefinition {

    FIRST_BLOOD("First Blood", "Write your first post", RarityTier.COMMON, "feather", true),
    SIDE_QUEST("Side Quest", "Leave your first comment", RarityTier.COMMON, "message", true),
    TOUCH_GRASS("Touch Grass", "Come back after a 3 day break", RarityTier.RARE, "leaf", true),
    SOCIAL_BUTTERFLY("Social Butterfly", "Collect 50 followers", RarityTier.RARE, "people", true),
    BINGE_WATCHER("Binge Watcher", "Finish 10 titles", RarityTier.RARE, "check", true),
    ON_REPEAT("On Repeat", "30 day listening streak", RarityTier.RARE, "loop", true),
    LORE_MASTER("Lore Master", "Write a review for 10 titles", RarityTier.EPIC, "book", true),
    CRITICAL_HIT("Critical Hit", "Collect 100 reactions", RarityTier.EPIC, "bolt", true),
    WHO_LET_YOU_COOK("Who Let You Cook", "50 reactions on a single post", RarityTier.EPIC, "flame", true),
    NIGHT_OWL("Night Owl", "Post between 3 and 4 AM", RarityTier.EPIC, "moon", true),
    MAIN_CHARACTER("Main Character", "Reach level 20", RarityTier.LEGENDARY, "crown", true),
    LEGEND("Legend", "Reach level 30", RarityTier.LEGENDARY, "trophy", true);

    public enum RarityTier {
        COMMON(50), RARE(100), EPIC(300), LEGENDARY(500);

        private final int bonusXp;

        RarityTier(int bonusXp) {
            this.bonusXp = bonusXp;
        }

        public int bonusXp() {
            return bonusXp;
        }
    }

    private final String displayName;
    private final String description;
    private final RarityTier rarity;
    private final String icon;
    private final boolean available;

    AchievementDefinition(String displayName, String description, RarityTier rarity, String icon,
                          boolean available) {
        this.displayName = displayName;
        this.description = description;
        this.rarity = rarity;
        this.icon = icon;
        this.available = available;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
    public RarityTier rarity() { return rarity; }
    public String icon() { return icon; }
    public boolean available() { return available; }
}
