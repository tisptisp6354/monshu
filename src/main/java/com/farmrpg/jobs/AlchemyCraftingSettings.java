package com.farmrpg.jobs;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-configurable test quantities and success chances; defaults are not final balance. */
public final class AlchemyCraftingSettings {
    private static final String[] RECIPES = {"life", "guard", "battle", "moonlight"};
    private static final String[] INGREDIENTS = {"main", "support_a", "support_b"};
    private static final int[] DEFAULT_HERBS = {3, 2, 1};
    private static final ModConfigSpec.IntValue[][] HERB_COUNTS = new ModConfigSpec.IntValue[4][3];
    private static final ModConfigSpec.IntValue[] CATALYST_COUNTS = new ModConfigSpec.IntValue[4];
    private static final ModConfigSpec.IntValue[] SUCCESS_CHANCES = new ModConfigSpec.IntValue[4];
    private static ModConfigSpec.IntValue waterPerPotion;

    private AlchemyCraftingSettings() {}

    static void register(ModConfigSpec.Builder builder) {
        builder.comment("Workbench ingredient counts and water use. These are test defaults.")
                .push("alchemy_workbench");
        waterPerPotion = builder.comment("Water in mB for each potion; proposed initial value.")
                .defineInRange("water_per_potion", 250, 1, GuildAlchemyWorkbenchBlockEntity.WATER_CAPACITY);
        for (int r = 0; r < RECIPES.length; r++) {
            builder.push(RECIPES[r]);
            for (int i = 0; i < INGREDIENTS.length; i++) {
                HERB_COUNTS[r][i] = builder.defineInRange(INGREDIENTS[i] + "_herb_count", DEFAULT_HERBS[i], 1, 64);
            }
            builder.pop();
        }
        builder.push("catalysts");
        int[] defaults = {2, 3, 2, 2};
        for (HerbGrade grade : HerbGrade.values()) {
            CATALYST_COUNTS[grade.modelId()] = builder.defineInRange(
                    grade.translationKey() + "_count", defaults[grade.modelId()], 1, 64);
        }
        builder.pop();
        builder.comment("Independent success percent per eligible crafting attempt. Zero disables success.")
                .push("success_chance_percent");
        int[] defaultChances = {100, 90, 75, 55};
        for (HerbGrade grade : HerbGrade.values()) {
            SUCCESS_CHANCES[grade.modelId()] = builder.defineInRange(
                    grade.translationKey(), defaultChances[grade.modelId()], 0, 100);
        }
        builder.pop();
        builder.pop();
    }

    public static int waterPerPotion() { return waterPerPotion.get(); }
    public static int herbCount(int recipe, int ingredient) { return HERB_COUNTS[recipe][ingredient].get(); }
    public static int catalystCount(HerbGrade grade) { return CATALYST_COUNTS[grade.modelId()].get(); }
    public static int successChance(HerbGrade grade) { return SUCCESS_CHANCES[grade.modelId()].get(); }
}
