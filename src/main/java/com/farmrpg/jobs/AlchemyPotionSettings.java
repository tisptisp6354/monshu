package com.farmrpg.jobs;

import java.util.EnumMap;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side test values. These defaults are not final balance decisions. */
public final class AlchemyPotionSettings {
    private record EffectSettings(ModConfigSpec.IntValue seconds, ModConfigSpec.IntValue level) {}

    private static final EnumMap<HerbGrade, EffectSettings> LIFE_POTION = new EnumMap<>(HerbGrade.class);
    private static final EnumMap<HerbGrade, EffectSettings> GUARD_POTION = new EnumMap<>(HerbGrade.class);
    private static final EnumMap<HerbGrade, EffectSettings> BATTLE_POTION = new EnumMap<>(HerbGrade.class);
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Temporary life potion test values; levels use 1 for Regeneration I.")
                .push("life_potion");
        addLifeSettings(builder, HerbGrade.COMMON, 8, 1);
        addLifeSettings(builder, HerbGrade.ADVANCED, 10, 2);
        addLifeSettings(builder, HerbGrade.RARE, 12, 3);
        addLifeSettings(builder, HerbGrade.HEROIC, 14, 4);
        builder.pop();

        addBuffSettings(builder, "guard_potion", GUARD_POTION);
        addBuffSettings(builder, "battle_potion", BATTLE_POTION);
        AlchemyCraftingSettings.register(builder);
        SPEC = builder.build();
    }

    private AlchemyPotionSettings() {}

    private static void addLifeSettings(ModConfigSpec.Builder builder, HerbGrade grade,
            int defaultSeconds, int defaultLevel) {
        String key = grade.translationKey();
        ModConfigSpec.IntValue seconds = builder.comment("Duration in seconds; temporary default.")
                .defineInRange(key + "_seconds", defaultSeconds, 1, 3600);
        ModConfigSpec.IntValue level = builder.comment("Regeneration level; temporary default.")
                .defineInRange(key + "_level", defaultLevel, 1, 5);
        LIFE_POTION.put(grade, new EffectSettings(seconds, level));
    }

    private static void addBuffSettings(ModConfigSpec.Builder builder, String section,
            EnumMap<HerbGrade, EffectSettings> settings) {
        builder.comment("Temporary test values; adjust after gameplay testing.").push(section);
        addBuffGrade(builder, settings, HerbGrade.COMMON, 8, 1);
        addBuffGrade(builder, settings, HerbGrade.ADVANCED, 10, 2);
        addBuffGrade(builder, settings, HerbGrade.RARE, 12, 3);
        addBuffGrade(builder, settings, HerbGrade.HEROIC, 14, 4);
        builder.pop();
    }

    private static void addBuffGrade(ModConfigSpec.Builder builder,
            EnumMap<HerbGrade, EffectSettings> settings, HerbGrade grade,
            int defaultSeconds, int defaultLevel) {
        String key = grade.translationKey();
        ModConfigSpec.IntValue seconds = builder.comment("Duration in seconds; temporary default.")
                .defineInRange(key + "_seconds", defaultSeconds, 1, 3600);
        ModConfigSpec.IntValue level = builder.comment("Effect level; temporary default.")
                .defineInRange(key + "_level", defaultLevel, 1, 5);
        settings.put(grade, new EffectSettings(seconds, level));
    }

    public static int lifeDurationTicks(HerbGrade grade) {
        return LIFE_POTION.get(grade).seconds().get() * 20;
    }

    public static int lifeAmplifier(HerbGrade grade) {
        return LIFE_POTION.get(grade).level().get() - 1;
    }

    public static int guardDurationTicks(HerbGrade grade) {
        return GUARD_POTION.get(grade).seconds().get() * 20;
    }

    public static int guardAmplifier(HerbGrade grade) {
        return GUARD_POTION.get(grade).level().get() - 1;
    }

    public static int battleDurationTicks(HerbGrade grade) {
        return BATTLE_POTION.get(grade).seconds().get() * 20;
    }

    public static int battleAmplifier(HerbGrade grade) {
        return BATTLE_POTION.get(grade).level().get() - 1;
    }
}
