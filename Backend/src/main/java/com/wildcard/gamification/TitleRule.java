package com.wildcard.gamification;

import java.util.List;

/**
 * The personality title is picked from the dominant stat plus the second
 * stat of the pair. Every title has its own flavor sentence.
 */
public final class TitleRule {

    private record Combo(XpCategory first, XpCategory second, String title, String flavor) {
    }

    private static final List<Combo> COMBOS = List.of(
            new Combo(XpCategory.ANI, XpCategory.CHA, "Chaotic Storyteller",
                    "Lives for the next episode, whatever it costs."),
            new Combo(XpCategory.ANI, XpCategory.MUS, "Anime Setlist DJ",
                    "Has the same four songs on repeat since 2019."),
            new Combo(XpCategory.ANI, XpCategory.GAM, "Shonen Prodigy",
                    "Screams at the screen. Every single time."),
            new Combo(XpCategory.GAM, XpCategory.CHA, "Tactical Duelist",
                    "Will lose a ranked match and blame the ping."),
            new Combo(XpCategory.GAM, XpCategory.MUS, "Loot & Listen",
                    "Drops more loot than anyone else on the roster."),
            new Combo(XpCategory.MUS, XpCategory.CHA, "Main Character Energy",
                    "Walks into a room and the soundtrack starts."),
            new Combo(XpCategory.CHA, XpCategory.ANI, "Rewatch Specialist",
                    "Starts a fourth season before finishing the first."),
            new Combo(XpCategory.CHA, XpCategory.GAM, "Roll Call Enthusiast",
                    "Has strong opinions about min-maxing."),
            new Combo(XpCategory.CHA, XpCategory.MUS, "Headphone Dweller",
                    "Listens to everything twice, on purpose."),
            new Combo(XpCategory.MUS, XpCategory.GAM, "Speedrun Dreamer",
                    "Practises one level until it becomes muscle memory."),
            new Combo(XpCategory.MUS, XpCategory.ANI, "Cafeteria Critic",
                    "Rates everything, including the weather."),
            new Combo(XpCategory.GAM, XpCategory.ANI, "Co-op Refugee",
                    "Says yes to every party invite. Regrets most of them.")
    );

    private static final String FALLBACK_TITLE = "Wild Card Rookie";
    private static final String FALLBACK_FLAVOR = "Potential is there. Nothing to prove yet.";

    private TitleRule() {
    }

    public static String pick(int ani, int gam, int mus, int cha) {
        Combo combo = pickCombo(ani, gam, mus, cha);
        return combo == null ? FALLBACK_TITLE : combo.title();
    }

    public static String flavorOf(String title) {
        for (Combo combo : COMBOS) {
            if (combo.title().equals(title)) {
                return combo.flavor();
            }
        }
        return FALLBACK_FLAVOR;
    }

    public static String flavor(int ani, int gam, int mus, int cha) {
        Combo combo = pickCombo(ani, gam, mus, cha);
        return combo == null ? FALLBACK_FLAVOR : combo.flavor();
    }

    public static int titleCount() {
        return COMBOS.size() + 1;
    }

    private static Combo pickCombo(int ani, int gam, int mus, int cha) {
        int[] stats = {ani, gam, mus, cha};
        XpCategory[] categories = XpCategory.values();

        if (ani + gam + mus + cha == 0) {
            return null;
        }

        int first = 0;
        int second = -1;
        for (int i = 1; i < stats.length; i++) {
            if (stats[i] > stats[first]) {
                second = first;
                first = i;
            } else if (second == -1 || stats[i] > stats[second]) {
                second = i;
            }
        }

        if (second == -1) {
            for (int i = 0; i < stats.length; i++) {
                if (i != first && (second == -1 || stats[i] < stats[second])) {
                    second = i;
                }
            }
        }
        if (second == -1 || second == first) {
            return null;
        }

        XpCategory a = categories[first];
        XpCategory b = categories[second];

        for (Combo combo : COMBOS) {
            if (combo.first() == a && combo.second() == b) {
                return combo;
            }
        }
        for (Combo combo : COMBOS) {
            if (combo.first() == b && combo.second() == a) {
                return combo;
            }
        }
        for (Combo combo : COMBOS) {
            if (combo.first() == a) {
                return combo;
            }
        }
        return null;
    }
}