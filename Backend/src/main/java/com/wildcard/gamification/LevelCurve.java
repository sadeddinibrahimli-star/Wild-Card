package com.wildcard.gamification;

public final class LevelCurve {

    private LevelCurve() {
    }

    /** Level from total XP. Same formula as CardService. */
    public static int levelFor(long totalXp) {
        if (totalXp <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.floor(Math.sqrt(totalXp / 100.0)));
    }

    public static long totalXpForLevel(int level) {
        int n = Math.max(1, level);
        return (long) (100L * n * n);
    }

    public static int xpIntoLevel(long totalXp) {
        int level = levelFor(totalXp);
        return (int) (totalXp - totalXpForLevel(level));
    }

    public static long totalXpForNextLevel(long totalXp) {
        return totalXpForLevel(levelFor(totalXp) + 1);
    }

    public static int xpForNext(long totalXp) {
        return (int) (totalXpForNextLevel(totalXp) - totalXp);
    }
}
