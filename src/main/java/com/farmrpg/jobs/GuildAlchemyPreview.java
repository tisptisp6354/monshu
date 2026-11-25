package com.farmrpg.jobs;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

/** Shared recipe identities and display stacks. Crafting validates and consumes separately. */
public final class GuildAlchemyPreview {
    public static final int RECIPE_COUNT = 4;

    private GuildAlchemyPreview() {}

    public static ItemStack herb(int recipe, int index, HerbGrade grade) {
        return herb(recipe, index, grade, AlchemyCraftingSettings.herbCount(recipe, index));
    }

    public static ItemStack herb(int recipe, int index, HerbGrade grade, int count) {
        Item item = switch (recipe) {
            case 0 -> switch (index) {
                case 0 -> ModItems.LIFE_HERB_ITEM.get();
                case 1 -> ModItems.VITALITY_HERB_ITEM.get();
                default -> ModItems.POISON_MIST_HERB_ITEM.get();
            };
            case 1 -> switch (index) {
                case 0 -> ModItems.FROST_HERB_ITEM.get();
                case 1 -> ModItems.STAR_MOSS_ITEM.get();
                default -> ModItems.LIFE_HERB_ITEM.get();
            };
            case 2 -> switch (index) {
                case 0 -> ModItems.SUN_HERB_ITEM.get();
                case 1 -> ModItems.GALE_HERB_ITEM.get();
                default -> ModItems.VITALITY_HERB_ITEM.get();
            };
            default -> switch (index) {
                case 0 -> ModItems.MOONLIGHT_HERB_ITEM.get();
                case 1 -> ModItems.FROST_HERB_ITEM.get();
                default -> ModItems.STAR_MOSS_ITEM.get();
            };
        };
        ItemStack stack = new ItemStack(item, count);
        if (grade.modelId() != 0) stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(grade.modelId()));
        return stack;
    }

    public static ItemStack catalyst(HerbGrade grade) {
        return catalyst(grade, AlchemyCraftingSettings.catalystCount(grade));
    }

    public static ItemStack catalyst(HerbGrade grade, int count) {
        return switch (grade) {
            case COMMON, ADVANCED -> new ItemStack(Items.REDSTONE, count);
            case RARE -> new ItemStack(Items.GLOWSTONE_DUST, count);
            case HEROIC -> new ItemStack(Items.BLAZE_POWDER, count);
        };
    }

    public static ItemStack potion(int recipe, HerbGrade grade) {
        return switch (recipe) {
            case 0 -> LifePotionItem.forGrade(ModItems.LIFE_POTION.get(), grade);
            case 1 -> AdditionalPotionItem.forGrade(ModItems.GUARD_POTION.get(), grade);
            case 2 -> AdditionalPotionItem.forGrade(ModItems.BATTLE_POTION.get(), grade);
            default -> AdditionalPotionItem.forGrade(ModItems.MOONLIGHT_POTION.get(), grade);
        };
    }

    public static ItemStack ingredient(int recipe, int index, HerbGrade grade) {
        int count = index < 3 ? AlchemyCraftingSettings.herbCount(recipe, index)
                : index == 3 ? AlchemyCraftingSettings.catalystCount(grade) : 1;
        return ingredient(recipe, index, grade, count);
    }

    /** Client displays server-synchronized counts without reading local config. */
    public static ItemStack ingredient(int recipe, int index, HerbGrade grade, int count) {
        if (index < 3) return herb(recipe, index, grade, count);
        return index == 3 ? catalyst(grade, count) : new ItemStack(Items.GLASS_BOTTLE);
    }
}
