package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FarmRPGJobs.MODID)
@PrefixGameTestTemplate(false)
public final class WorkbenchCraftingGameTests {
    private static final BlockPos SUPPORT = new BlockPos(1, 1, 1);
    private static final BlockPos ROOT = SUPPORT.above();

    private static GuildAlchemyWorkbenchBlockEntity place(GameTestHelper helper, Player player) {
        BlockPos absolute = helper.absolutePos(ROOT);
        player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 1.5);
        helper.setBlock(SUPPORT, Blocks.STONE);
        player.setYRot(Direction.SOUTH.toYRot());
        ItemStack stack = new ItemStack(ModItems.GUILD_ALCHEMY_WORKBENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.placeAt(player, stack, SUPPORT, Direction.UP);
        helper.assertBlockPresent(ModBlocks.GUILD_ALCHEMY_WORKBENCH.get(), ROOT);
        return helper.getBlockEntity(ROOT);
    }

    private static GuildAlchemyWorkbenchMenu menu(Player player, GuildAlchemyWorkbenchBlockEntity bench, int id) {
        GuildAlchemyWorkbenchMenu menu = new GuildAlchemyWorkbenchMenu(id, player.getInventory(), bench);
        player.containerMenu = menu;
        return menu;
    }

    private static void prepare(GuildAlchemyWorkbenchBlockEntity bench, int recipe, int grade, int extra) {
        var plan = GuildAlchemyCrafting.plan(recipe, grade);
        for (int i = 0; i < 5; i++) {
            bench.inventory().setStackInSlot(i, plan.ingredients()[i].copyWithCount(plan.ingredients()[i].getCount() + extra));
        }
        bench.inventory().setStackInSlot(5, ItemStack.EMPTY);
    }

    private static void expectRejected(GameTestHelper helper, Player player, GuildAlchemyWorkbenchBlockEntity bench,
            GuildAlchemyWorkbenchMenu menu, String reason) {
        CompoundTag before = bench.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertFalse(menu.clickMenuButton(player, GuildAlchemyWorkbenchMenu.CRAFT_BUTTON), reason);
        helper.assertTrue(before.equals(bench.saveWithoutMetadata(helper.getLevel().registryAccess())),
                "Rejected crafting must not change any item or water: " + reason);
    }

    @GameTest(template = "workbench_test")
    public static void allSixteenRecipes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuildAlchemyWorkbenchBlockEntity bench = place(helper, player);
        GuildAlchemyWorkbenchMenu menu = menu(player, bench, 1);
        for (int i = 0; i < 4; i++) helper.assertTrue(bench.fillWaterBucket(), "Four buckets fit");
        Item[][] herbs = {
                {ModItems.LIFE_HERB_ITEM.get(), ModItems.VITALITY_HERB_ITEM.get(), ModItems.POISON_MIST_HERB_ITEM.get()},
                {ModItems.FROST_HERB_ITEM.get(), ModItems.STAR_MOSS_ITEM.get(), ModItems.LIFE_HERB_ITEM.get()},
                {ModItems.SUN_HERB_ITEM.get(), ModItems.GALE_HERB_ITEM.get(), ModItems.VITALITY_HERB_ITEM.get()},
                {ModItems.MOONLIGHT_HERB_ITEM.get(), ModItems.FROST_HERB_ITEM.get(), ModItems.STAR_MOSS_ITEM.get()}
        };
        Item[] catalysts = {Items.REDSTONE, Items.REDSTONE, Items.GLOWSTONE_DUST, Items.BLAZE_POWDER};
        int[] catalystCounts = {2, 3, 2, 2};
        Item[] potions = {ModItems.LIFE_POTION.get(), ModItems.GUARD_POTION.get(),
                ModItems.BATTLE_POTION.get(), ModItems.MOONLIGHT_POTION.get()};
        int crafts = 0;
        for (int recipe = 0; recipe < 4; recipe++) {
            helper.assertTrue(menu.clickMenuButton(player, recipe), "Select recipe");
            for (int grade = 0; grade < 4; grade++) {
                helper.assertTrue(menu.clickMenuButton(player, GuildAlchemyWorkbenchMenu.GRADE_BUTTON_BASE + grade), "Select grade");
                var plan = GuildAlchemyCrafting.plan(recipe, grade);
                helper.assertTrue(plan.water() == 250, "Initial configurable water amount is 250 mB");
                for (int i = 0; i < 3; i++) {
                    helper.assertTrue(plan.ingredients()[i].is(herbs[recipe][i])
                            && plan.ingredients()[i].getCount() == 3 - i
                            && MoonlightHerbItem.gradeOf(plan.ingredients()[i]).modelId() == grade,
                            "Recipe uses required herb, grade and initial quantity");
                }
                helper.assertTrue(plan.ingredients()[3].is(catalysts[grade])
                        && plan.ingredients()[3].getCount() == catalystCounts[grade], "Correct grade catalyst");
                helper.assertTrue(plan.ingredients()[4].is(Items.GLASS_BOTTLE)
                        && plan.ingredients()[4].getCount() == 1, "An empty bottle is separate");
                prepare(bench, recipe, grade, 1);
                helper.assertTrue(menu.canCraft(), "Complete recipe is ready");
                helper.assertTrue(menu.clickMenuButton(player, GuildAlchemyWorkbenchMenu.CRAFT_BUTTON), "Craft exactly one potion");
                crafts++;
                for (int i = 0; i < 5; i++) {
                    helper.assertTrue(bench.inventory().getStackInSlot(i).getCount() == 1,
                            "Only the required ingredient quantity is consumed");
                }
                ItemStack result = bench.inventory().getStackInSlot(5);
                helper.assertTrue(result.is(potions[recipe]) && result.getCount() == 1
                        && MoonlightHerbItem.gradeOf(result).modelId() == grade, "Potion type and grade match");
                helper.assertTrue(bench.waterAmount() == 4000 - 250 * crafts, "Each craft consumes exactly 250 mB");
            }
        }
        helper.assertTrue(bench.waterAmount() == 0, "Sixteen crafts use exactly 4000 mB");
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void rejectionDoesNotConsume(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuildAlchemyWorkbenchBlockEntity bench = place(helper, player);
        GuildAlchemyWorkbenchMenu menu = menu(player, bench, 2);
        helper.assertTrue(bench.fillWaterBucket(), "Water fits");
        prepare(bench, 0, 0, 0);
        bench.inventory().setStackInSlot(0, new ItemStack(ModItems.SUN_HERB_ITEM.get(), 3));
        expectRejected(helper, player, bench, menu, "wrong herb");
        prepare(bench, 0, 0, 0);
        ItemStack wrongGrade = bench.inventory().getStackInSlot(0).copy();
        wrongGrade.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        bench.inventory().setStackInSlot(0, wrongGrade);
        expectRejected(helper, player, bench, menu, "wrong herb grade");
        prepare(bench, 0, 0, 0);
        bench.inventory().setStackInSlot(0, new ItemStack(ModItems.LIFE_HERB_ITEM.get(), 2));
        expectRejected(helper, player, bench, menu, "too few herbs");
        prepare(bench, 0, 0, 0);
        bench.inventory().setStackInSlot(3, new ItemStack(Items.BLAZE_POWDER, 2));
        expectRejected(helper, player, bench, menu, "wrong catalyst");
        prepare(bench, 0, 0, 0);
        bench.inventory().setStackInSlot(3, new ItemStack(Items.REDSTONE, 1));
        expectRejected(helper, player, bench, menu, "too little catalyst");
        prepare(bench, 0, 0, 0);
        bench.inventory().setStackInSlot(4, ItemStack.EMPTY);
        expectRejected(helper, player, bench, menu, "missing empty bottle");
        prepare(bench, 0, 0, 0);
        helper.assertTrue(bench.drainWater(1000, false), "Empty the tank");
        expectRejected(helper, player, bench, menu, "missing water");
        helper.assertTrue(bench.fillWaterBucket(), "Refill tank");
        bench.inventory().setStackInSlot(5, new ItemStack(ModItems.GUARD_POTION.get()));
        expectRejected(helper, player, bench, menu, "different output type");
        bench.inventory().setStackInSlot(5, LifePotionItem.forGrade(ModItems.LIFE_POTION.get(), HerbGrade.ADVANCED));
        expectRejected(helper, player, bench, menu, "different output grade");
        ItemStack full = LifePotionItem.forGrade(ModItems.LIFE_POTION.get(), HerbGrade.COMMON);
        full.setCount(64);
        bench.inventory().setStackInSlot(5, full);
        expectRejected(helper, player, bench, menu, "full output");
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void simultaneousRequestsAndOutputStacking(GameTestHelper helper) {
        Player alice = helper.makeMockPlayer(GameType.SURVIVAL);
        GuildAlchemyWorkbenchBlockEntity bench = place(helper, alice);
        Player bob = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos p = helper.absolutePos(ROOT);
        bob.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 1.5);
        GuildAlchemyWorkbenchMenu first = menu(alice, bench, 1);
        GuildAlchemyWorkbenchMenu second = menu(bob, bench, 2);
        helper.assertTrue(bench.fillWaterBucket(), "Water fits");
        prepare(bench, 0, 0, 0);
        helper.assertTrue(first.clickMenuButton(alice, GuildAlchemyWorkbenchMenu.CRAFT_BUTTON), "First player crafts");
        expectRejected(helper, bob, bench, second, "Second player cannot reuse consumed ingredients");
        for (int i = 0; i < 10; i++) expectRejected(helper, alice, bench, first, "Repeated clicks without resources");
        helper.assertTrue(bench.inventory().getStackInSlot(5).getCount() == 1 && bench.waterAmount() == 750,
                "Only one result and one water charge exist");
        prepare(bench, 0, 0, 3);
        ItemStack almostFull = LifePotionItem.forGrade(ModItems.LIFE_POTION.get(), HerbGrade.COMMON);
        almostFull.setCount(63);
        bench.inventory().setStackInSlot(5, almostFull);
        helper.assertTrue(second.clickMenuButton(bob, GuildAlchemyWorkbenchMenu.CRAFT_BUTTON), "A matching stack can fill to 64");
        helper.assertTrue(bench.inventory().getStackInSlot(5).getCount() == 64 && bench.waterAmount() == 500,
                "One result stacks and one water cost is consumed");
        helper.assertTrue(bench.craftingStatus(0, 0) == GuildAlchemyCrafting.Status.OUTPUT,
                "Only the full output blocks a second craft");
        expectRejected(helper, alice, bench, first, "A full output blocks crafting without consumption");
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void edgeMaskPreservesInterior(GameTestHelper helper) {
        int width = 64, height = 64;
        int[] pixels = new int[width * height];
        java.util.Arrays.fill(pixels, 0xFF0D0505);
        pixels[32 * width] = 0xFFFF3833;
        int center = pixels[32 * width + 32];
        GuildAlchemyEdgeMask.apply(pixels, width, height);
        helper.assertTrue((pixels[0] >>> 24) == 0, "Outside black should become transparent");
        helper.assertTrue(pixels[32 * width] == 0xFFFF3833, "Bright decoration at edge must remain");
        helper.assertTrue(pixels[32 * width + 32] == center, "Dark interior must remain opaque");
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void configurableRecipeAmounts(GameTestHelper helper) {
        var values = AlchemyPotionSettings.SPEC.getValues();
        var water = (net.neoforged.neoforge.common.ModConfigSpec.IntValue) values.get("alchemy_workbench.water_per_potion");
        var main = (net.neoforged.neoforge.common.ModConfigSpec.IntValue) values.get("alchemy_workbench.life.main_herb_count");
        var catalyst = (net.neoforged.neoforge.common.ModConfigSpec.IntValue) values.get("alchemy_workbench.catalysts.common_count");
        int oldWater = water.get(), oldMain = main.get(), oldCatalyst = catalyst.get();
        try {
            water.set(600);
            main.set(4);
            catalyst.set(5);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            GuildAlchemyWorkbenchBlockEntity bench = place(helper, player);
            GuildAlchemyWorkbenchMenu menu = menu(player, bench, 3);
            helper.assertTrue(bench.fillWaterBucket(), "Water fits");
            prepare(bench, 0, 0, 0);
            helper.assertTrue(menu.requiredCount(0) == 4 && menu.requiredCount(3) == 5 && menu.waterCost() == 600,
                    "The menu exposes the server's configured quantities");
            helper.assertTrue(menu.clickMenuButton(player, GuildAlchemyWorkbenchMenu.CRAFT_BUTTON), "Configured recipe crafts");
            helper.assertTrue(bench.waterAmount() == 400 && bench.inventory().getStackInSlot(0).isEmpty()
                    && bench.inventory().getStackInSlot(3).isEmpty(), "Configured quantities are consumed exactly");
        } finally {
            water.set(oldWater);
            main.set(oldMain);
            catalyst.set(oldCatalyst);
        }
        helper.succeed();
    }
}
