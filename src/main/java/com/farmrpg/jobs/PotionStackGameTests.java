package com.farmrpg.jobs;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FarmRPGJobs.MODID)
@PrefixGameTestTemplate(false)
public final class PotionStackGameTests {
    private static ItemStack graded(Item item, HerbGrade grade, int count) {
        ItemStack stack = item instanceof LifePotionItem life
                ? LifePotionItem.forGrade(life, grade)
                : AdditionalPotionItem.forGrade((AdditionalPotionItem) item, grade);
        stack.setCount(count);
        return stack;
    }

    @GameTest(template = "potion_stack_test")
    public static void potionStacksAndUse(GameTestHelper helper) {
        Item[] items = {ModItems.LIFE_POTION.get(), ModItems.GUARD_POTION.get(),
                ModItems.BATTLE_POTION.get(), ModItems.MOONLIGHT_POTION.get()};
        Player survival = helper.makeMockPlayer(GameType.SURVIVAL);
        survival.getAbilities().instabuild = false;
        Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;

        for (Item item : items) {
            for (HerbGrade grade : HerbGrade.values()) {
                ItemStack stack = graded(item, grade, 63);
                ItemStack same = graded(item, grade, 3);
                helper.assertTrue(stack.getMaxStackSize() == 64, "max stack size must be 64");
                helper.assertTrue(ItemStack.isSameItemSameComponents(stack, same), "identical grades should combine");

                survival.getInventory().clearContent();
                helper.assertTrue(survival.getInventory().add(stack), "cannot add first stack");
                helper.assertTrue(survival.getInventory().add(same), "cannot add second stack");
                helper.assertTrue(survival.getInventory().getItem(0).getCount() == 64, "first stack must reach 64");
                helper.assertTrue(survival.getInventory().getItem(1).getCount() == 2, "overflow must retain 2");
                ItemStack full = survival.getInventory().getItem(0);
                Tag saved = full.save(helper.getLevel().registryAccess());
                ItemStack restored = ItemStack.parse(helper.getLevel().registryAccess(), saved).orElseThrow();
                helper.assertTrue(restored.getCount() == 64 && ItemStack.isSameItemSameComponents(full, restored),
                        "64-stack with grade must serialize and reload");

                for (HerbGrade otherGrade : HerbGrade.values()) {
                    if (grade != otherGrade) {
                        ItemStack other = graded(item, otherGrade, 1);
                        helper.assertFalse(ItemStack.isSameItemSameComponents(full, other), "different grades must stay separate");
                    }
                }

                for (int count : new int[]{1, 2, 64}) {
                    for (Player player : new Player[]{survival, creative}) {
                        player.getInventory().clearContent();
                        player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE, 2));
                        player.removeAllEffects();
                        ItemStack inHand = graded(item, grade, count);
                        ItemStack result = item.finishUsingItem(inHand, helper.getLevel(), player);
                        boolean inactive = item == ModItems.MOONLIGHT_POTION.get();
                        int expected = inactive || player.hasInfiniteMaterials() ? count : count - 1;
                        helper.assertTrue(result.getCount() == expected, "drinking must consume exactly one in survival");
                        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 2,
                                "drinking must not add or delete glass bottles");
                        if (!inactive) {
                            if (expected > 0) helper.assertTrue(MoonlightHerbItem.gradeOf(result) == grade, "grade must be retained");
                            if (item == ModItems.LIFE_POTION.get()) helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "missing regeneration");
                            if (item == ModItems.GUARD_POTION.get()) helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "missing resistance");
                            if (item == ModItems.BATTLE_POTION.get()) helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_BOOST), "missing strength");
                        } else {
                            player.setItemInHand(InteractionHand.MAIN_HAND, inHand);
                            helper.assertTrue(item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL,
                                    "inactive mana potion must not be drinkable");
                        }
                    }
                }
            }
        }
        ItemStack life = graded(items[0], HerbGrade.COMMON, 1);
        ItemStack guard = graded(items[1], HerbGrade.COMMON, 1);
        helper.assertFalse(ItemStack.isSameItemSameComponents(life, guard), "different potion types must not combine");
        helper.succeed();
    }
}
