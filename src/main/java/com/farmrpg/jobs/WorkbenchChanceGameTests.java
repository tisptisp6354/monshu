package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FarmRPGJobs.MODID)
@PrefixGameTestTemplate(false)
public final class WorkbenchChanceGameTests {
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

    private static void prepare(GuildAlchemyWorkbenchBlockEntity bench, int recipe, int grade, int count) {
        var plan = GuildAlchemyCrafting.plan(recipe, grade);
        for (int i = 0; i < 5; i++) {
            bench.inventory().setStackInSlot(i, plan.ingredients()[i].copyWithCount(count));
        }
        bench.inventory().setStackInSlot(5, ItemStack.EMPTY);
    }

    @GameTest(template = "workbench_test")
    public static void defaultProbabilitiesAndBoundaries(GameTestHelper helper) {
        int[] expected = {100, 90, 75, 55};
        for (HerbGrade grade : HerbGrade.values()) {
            int probability = AlchemyCraftingSettings.successChance(grade);
            helper.assertTrue(probability == expected[grade.modelId()], "Default grade probability must match the decision");
            for (int roll = 0; roll < 100; roll++) {
                helper.assertTrue(GuildAlchemyCrafting.succeeds(roll, probability) == (roll < probability),
                        "The probability boundary uses one fresh 0..99 roll");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void failureConsumesOnlyAgreedIngredients(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuildAlchemyWorkbenchBlockEntity bench = place(helper, player);
        for (int i = 0; i < 4; i++) helper.assertTrue(bench.fillWaterBucket(), "Four buckets fit");
        int attempts = 0;
        for (int recipe = 0; recipe < 4; recipe++) {
            for (int grade = 1; grade < 4; grade++) {
                prepare(bench, recipe, grade, 64);
                ItemStack oldResult = GuildAlchemyCrafting.plan(recipe, grade).potion().copyWithCount(2);
                bench.inventory().setStackInSlot(5, oldResult.copy());
                final int[] calls = {0};
                var chunk = helper.getLevel().getChunkAt(helper.absolutePos(ROOT));
                chunk.setUnsaved(false);
                var outcome = bench.craft(recipe, grade, () -> { calls[0]++; return 99; });
                attempts++;
                helper.assertTrue(outcome == GuildAlchemyCrafting.Outcome.FAILURE && calls[0] == 1,
                        "An eligible failure draws exactly once");
                helper.assertTrue(bench.inventory().getStackInSlot(0).getCount() == 64
                        && bench.inventory().getStackInSlot(4).getCount() == 64, "Main herb and bottles remain");
                helper.assertTrue(bench.inventory().getStackInSlot(1).getCount() == 62
                        && bench.inventory().getStackInSlot(2).getCount() == 63, "Only the required support amounts are used");
                helper.assertTrue(bench.inventory().getStackInSlot(3).getCount() == (grade == 1 ? 61 : 62),
                        "Only the required grade catalyst amount is used");
                helper.assertTrue(ItemStack.isSameItemSameComponents(bench.inventory().getStackInSlot(5), oldResult)
                        && bench.inventory().getStackInSlot(5).getCount() == 2, "Existing output stays unchanged");
                helper.assertTrue(bench.waterAmount() == 4000 - 250 * attempts, "Exactly 250 mB is consumed per failure");
                helper.assertTrue(chunk.isUnsaved(), "The failed attempt is persisted");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void separateAttemptsAndRejectedRequest(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuildAlchemyWorkbenchBlockEntity bench = place(helper, player);
        helper.assertTrue(bench.fillWaterBucket(), "Water fits");
        prepare(bench, 0, 3, 6);
        var oldResult = GuildAlchemyCrafting.plan(0, 3).potion().copyWithCount(2);
        bench.inventory().setStackInSlot(5, oldResult.copy());
        int[] draws = {0};
        var failure = bench.craft(0, 3, () -> { draws[0]++; return 55; });
        helper.assertTrue(failure == GuildAlchemyCrafting.Outcome.FAILURE && draws[0] == 1,
                "55 is a failure at a 55 percent chance");
        helper.assertTrue(bench.inventory().getStackInSlot(0).getCount() == 6
                && bench.inventory().getStackInSlot(1).getCount() == 4
                && bench.inventory().getStackInSlot(2).getCount() == 5
                && bench.inventory().getStackInSlot(3).getCount() == 4
                && bench.inventory().getStackInSlot(4).getCount() == 6
                && bench.inventory().getStackInSlot(5).getCount() == 2
                && bench.waterAmount() == 750, "A failed attempt has exactly the agreed costs");
        var success = bench.craft(0, 3, () -> { draws[0]++; return 54; });
        helper.assertTrue(success == GuildAlchemyCrafting.Outcome.SUCCESS && draws[0] == 2,
                "The next attempt makes a new independent roll and can succeed");
        helper.assertTrue(bench.inventory().getStackInSlot(0).getCount() == 3
                && bench.inventory().getStackInSlot(1).getCount() == 2
                && bench.inventory().getStackInSlot(2).getCount() == 4
                && bench.inventory().getStackInSlot(3).getCount() == 2
                && bench.inventory().getStackInSlot(4).getCount() == 5
                && bench.inventory().getStackInSlot(5).getCount() == 3
                && bench.waterAmount() == 500, "Success consumes full ingredients and adds one matching potion");
        bench.inventory().setStackInSlot(1, ItemStack.EMPTY);
        var before = bench.saveWithoutMetadata(helper.getLevel().registryAccess());
        var rejected = bench.craft(0, 3, () -> { draws[0]++; return 0; });
        helper.assertTrue(rejected == GuildAlchemyCrafting.Outcome.REJECTED && draws[0] == 2,
                "Missing ingredients reject before rolling");
        helper.assertTrue(before.equals(bench.saveWithoutMetadata(helper.getLevel().registryAccess())),
                "A rejected request consumes nothing");
        prepare(bench, 0, 3, 6);
        bench.inventory().setStackInSlot(5, GuildAlchemyCrafting.plan(0, 3).potion().copyWithCount(64));
        before = bench.saveWithoutMetadata(helper.getLevel().registryAccess());
        rejected = bench.craft(0, 3, () -> { draws[0]++; return 0; });
        helper.assertTrue(rejected == GuildAlchemyCrafting.Outcome.REJECTED && draws[0] == 2,
                "A full output rejects before rolling");
        helper.assertTrue(before.equals(bench.saveWithoutMetadata(helper.getLevel().registryAccess())),
                "A full output consumes nothing");
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void menuReportsEachAttempt(GameTestHelper helper) {
        var chance = WorkbenchCraftingGameTests.chanceValue(HerbGrade.HEROIC);
        int old = chance.get();
        try {
            chance.set(0); // Deterministic test configuration; production default stays 55.
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            GuildAlchemyWorkbenchBlockEntity bench = place(helper, player);
            helper.assertTrue(bench.fillWaterBucket(), "Water fits");
            prepare(bench, 0, 3, 6);
            var menu = new GuildAlchemyWorkbenchMenu(80, player.getInventory(), bench);
            var client = new GuildAlchemyWorkbenchMenu(80, player.getInventory());
            menu.setSynchronizer(new net.minecraft.world.inventory.ContainerSynchronizer() {
                @Override public void sendInitialData(net.minecraft.world.inventory.AbstractContainerMenu source,
                        net.minecraft.core.NonNullList<ItemStack> stacks, ItemStack carried, int[] values) {
                    for (int i = 0; i < values.length; i++) client.setData(i, values[i]);
                }
                @Override public void sendSlotChange(net.minecraft.world.inventory.AbstractContainerMenu source, int slot, ItemStack stack) {}
                @Override public void sendCarriedChange(net.minecraft.world.inventory.AbstractContainerMenu source, ItemStack stack) {}
                @Override public void sendDataChange(net.minecraft.world.inventory.AbstractContainerMenu source, int index, int value) {
                    client.setData(index, value);
                }
            });
            player.containerMenu = menu;
            helper.assertTrue(menu.clickMenuButton(player, GuildAlchemyWorkbenchMenu.GRADE_BUTTON_BASE + 3), "Select heroic");
            menu.broadcastChanges();
            helper.assertTrue(menu.successChance() == 0, "The GUI receives the server-configured chance");
            helper.assertTrue(client.selectedGrade() == 3 && client.successChance() == 0,
                    "Selection and chance are sent through the menu synchronizer");
            for (int i = 1; i <= 2; i++) {
                helper.assertTrue(menu.clickMenuButton(player, GuildAlchemyWorkbenchMenu.CRAFT_BUTTON), "Failure is still an accepted attempt");
                menu.broadcastChanges();
                helper.assertTrue(menu.lastOutcome() == GuildAlchemyCrafting.Outcome.FAILURE.ordinal()
                        && menu.attemptSequence() == i, "Each failure is sent to the viewing player");
                helper.assertTrue(client.lastOutcome() == GuildAlchemyCrafting.Outcome.FAILURE.ordinal()
                        && client.attemptSequence() == i, "Each failure reaches the client menu");
            }
            helper.assertTrue(bench.waterAmount() == 500 && bench.inventory().getStackInSlot(0).getCount() == 6
                    && bench.inventory().getStackInSlot(4).getCount() == 6, "Repeated failures protect the main herb and bottles");
            chance.set(100);
            helper.assertTrue(menu.successChance() == 100, "Updated server chance is visible");
            helper.assertTrue(menu.clickMenuButton(player, GuildAlchemyWorkbenchMenu.CRAFT_BUTTON), "Next valid request can succeed");
            menu.broadcastChanges();
            helper.assertTrue(menu.lastOutcome() == GuildAlchemyCrafting.Outcome.SUCCESS.ordinal()
                    && menu.attemptSequence() == 3 && bench.inventory().getStackInSlot(5).getCount() == 1,
                    "Success is sent and produces one potion");
            helper.assertTrue(client.successChance() == 100 && client.lastOutcome() == GuildAlchemyCrafting.Outcome.SUCCESS.ordinal()
                    && client.attemptSequence() == 3, "The changed chance and success reach the client menu");
        } finally {
            chance.set(old);
        }
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void oldConfigPreservesExistingValues(GameTestHelper helper) {
        var old = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        old.set("life_potion.common_seconds", 19);
        old.set("alchemy_workbench.water_per_potion", 375);
        old.set("alchemy_workbench.life.main_herb_count", 4);
        old.set("alchemy_workbench.catalysts.heroic_count", 3);
        AlchemyPotionSettings.SPEC.correct(old);
        helper.assertTrue(Integer.valueOf(19).equals(old.get("life_potion.common_seconds"))
                && Integer.valueOf(375).equals(old.get("alchemy_workbench.water_per_potion"))
                && Integer.valueOf(4).equals(old.get("alchemy_workbench.life.main_herb_count"))
                && Integer.valueOf(3).equals(old.get("alchemy_workbench.catalysts.heroic_count")),
                "Existing valid effect and recipe settings are preserved");
        helper.assertTrue(Integer.valueOf(100).equals(old.get("alchemy_workbench.success_chance_percent.common"))
                && Integer.valueOf(90).equals(old.get("alchemy_workbench.success_chance_percent.advanced"))
                && Integer.valueOf(75).equals(old.get("alchemy_workbench.success_chance_percent.rare"))
                && Integer.valueOf(55).equals(old.get("alchemy_workbench.success_chance_percent.heroic")),
                "Missing chances receive the agreed defaults");
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void twoViewersCannotReuseFailedIngredients(GameTestHelper helper) {
        var chance = WorkbenchCraftingGameTests.chanceValue(HerbGrade.HEROIC);
        int old = chance.get();
        try {
            chance.set(0);
            Player firstPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
            GuildAlchemyWorkbenchBlockEntity bench = place(helper, firstPlayer);
            Player secondPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos p = helper.absolutePos(ROOT);
            secondPlayer.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 1.5);
            helper.assertTrue(bench.fillWaterBucket(), "Water fits");
            prepare(bench, 0, 3, 3);
            var first = new GuildAlchemyWorkbenchMenu(82, firstPlayer.getInventory(), bench);
            var second = new GuildAlchemyWorkbenchMenu(83, secondPlayer.getInventory(), bench);
            firstPlayer.containerMenu = first;
            secondPlayer.containerMenu = second;
            helper.assertTrue(first.clickMenuButton(firstPlayer, GuildAlchemyWorkbenchMenu.GRADE_BUTTON_BASE + 3), "First selects heroic");
            helper.assertTrue(second.clickMenuButton(secondPlayer, GuildAlchemyWorkbenchMenu.GRADE_BUTTON_BASE + 3), "Second selects heroic");
            helper.assertTrue(first.clickMenuButton(firstPlayer, GuildAlchemyWorkbenchMenu.CRAFT_BUTTON), "First failure is processed");
            var after = bench.saveWithoutMetadata(helper.getLevel().registryAccess());
            helper.assertFalse(second.clickMenuButton(secondPlayer, GuildAlchemyWorkbenchMenu.CRAFT_BUTTON),
                    "The other viewer cannot reuse spent secondary materials");
            helper.assertTrue(after.equals(bench.saveWithoutMetadata(helper.getLevel().registryAccess()))
                    && bench.waterAmount() == 750 && bench.inventory().getStackInSlot(0).getCount() == 3
                    && bench.inventory().getStackInSlot(4).getCount() == 3 && second.attemptSequence() == 0,
                    "The rejected second request changes nothing and the protected materials remain");
        } finally {
            chance.set(old);
        }
        helper.succeed();
    }
}
