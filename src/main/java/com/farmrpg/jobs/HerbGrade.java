package com.farmrpg.jobs;

/** Grade values stored in the existing item's custom model data. */
public enum HerbGrade {
    COMMON("common", 0),
    ADVANCED("advanced", 1),
    RARE("rare", 2),
    HEROIC("heroic", 3);

    private final String translationKey;
    private final int modelId;

    HerbGrade(String translationKey, int modelId) {
        this.translationKey = translationKey;
        this.modelId = modelId;
    }

    public String translationKey() {
        return translationKey;
    }

    public int modelId() {
        return modelId;
    }

    public static HerbGrade fromModelId(int value) {
        if (value >= 3) return HEROIC;
        if (value == 2) return RARE;
        if (value == 1) return ADVANCED;
        return COMMON;
    }

    /** Each argument must be an independent uniformly sampled integer from 0 through 99. */
    public static HerbGrade fromRoll(boolean herbalist, int roll) {
        checkRoll(roll);
        if (roll < (herbalist ? 55 : 70)) return COMMON;
        if (roll < (herbalist ? 80 : 90)) return ADVANCED;
        if (roll < (herbalist ? 95 : 98)) return RARE;
        return HEROIC;
    }

    public static boolean grantsBonus(int roll) {
        checkRoll(roll);
        return roll < 30;
    }

    private static void checkRoll(int roll) {
        if (roll < 0 || roll >= 100) {
            throw new IllegalArgumentException("Roll must be from 0 to 99");
        }
    }
}
