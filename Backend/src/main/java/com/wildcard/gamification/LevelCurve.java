package com.wildcard.gamification;

/**
 * Səviyyə əyrisi. MÖVCUD SİSTEM DƏYİŞMİR — bu yalnız
 * "bu səviyyəyə nə qədər XP lazımdır" məlumatını verir.
 */
public final class LevelCurve {

    private LevelCurve() {
    }

    /** Ümumi XP-dən səviyyə. CardService ilə eyni formulu. */
    public static int levelFor(long totalXp) {
        if (totalXp <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.floor(Math.sqrt(totalXp / 100.0)));
    }

    /** Bu səviyyəyə çatmaq üçün lazım olan ümumi XP. */
    public static long totalXpForLevel(int level) {
        int n = Math.max(1, level);
        return (long) (100L * n * n);
    }

    /** Cari səviyyəyə keçəndən bəri toplanan XP. */
    public static int xpIntoLevel(long totalXp) {
        int level = levelFor(totalXp);
        return (int) (totalXp - totalXpForLevel(level));
    }

    /** Növbəti səviyyəyə çatacaq ümumi XP. */
    public static long totalXpForNextLevel(long totalXp) {
        return totalXpForLevel(levelFor(totalXp) + 1);
    }

    /** Növbəti səviyyə üçün lazım olan XP (hazırkı səviyyədən). */
    public static int xpForNext(long totalXp) {
        return (int) (totalXpForNextLevel(totalXp) - totalXp);
    }
}
