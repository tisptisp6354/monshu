package com.farmrpg.jobs;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** A server-side snapshot of the chosen recipe and its configurable quantities. */
final class GuildAlchemyCrafting {
    enum Status { READY, HERBS, CATALYST, BOTTLE, WATER, OUTPUT, INVALID }

    record Plan(ItemStack[] ingredients, int water, ItemStack potion, HerbGrade grade) {}

    private GuildAlchemyCrafting() {}

    static Plan plan(int recipe, int gradeId) {
        if (recipe < 0 || recipe >= GuildAlchemyPreview.RECIPE_COUNT
                || gradeId < 0 || gradeId >= HerbGrade.values().length) return null;
        HerbGrade grade = HerbGrade.fromModelId(gradeId);
        ItemStack[] ingredients = new ItemStack[5];
        for (int i = 0; i < ingredients.length; i++) {
            ingredients[i] = GuildAlchemyPreview.ingredient(recipe, i, grade);
        }
        return new Plan(ingredients, AlchemyCraftingSettings.waterPerPotion(),
                GuildAlchemyPreview.potion(recipe, grade), grade);
    }

    static Status status(Plan plan, IItemHandler inventory, int water) {
        if (plan == null) return Status.INVALID;
        for (int i = 0; i < plan.ingredients().length; i++) {
            ItemStack expected = plan.ingredients()[i];
            ItemStack actual = inventory.getStackInSlot(i);
            if (!actual.is(expected.getItem()) || actual.getCount() < expected.getCount()
                    || (i < 3 && MoonlightHerbItem.gradeOf(actual) != plan.grade())) {
                return i < 3 ? Status.HERBS : i == 3 ? Status.CATALYST : Status.BOTTLE;
            }
        }
        if (water < plan.water()) return Status.WATER;
        ItemStack output = inventory.getStackInSlot(GuildAlchemyWorkbenchBlockEntity.OUTPUT_SLOT);
        ItemStack result = plan.potion();
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
                || output.getCount() >= Math.min(inventory.getSlotLimit(GuildAlchemyWorkbenchBlockEntity.OUTPUT_SLOT),
                        output.getMaxStackSize()))) return Status.OUTPUT;
        return Status.READY;
    }
}
