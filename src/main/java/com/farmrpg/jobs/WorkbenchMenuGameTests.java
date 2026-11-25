package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
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
public final class WorkbenchMenuGameTests {
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

    private static GuildAlchemyWorkbenchMenu open(Player player, GuildAlchemyWorkbenchBlockEntity bench, int id) {
        GuildAlchemyWorkbenchMenu menu = new GuildAlchemyWorkbenchMenu(id, player.getInventory(), bench);
        player.containerMenu = menu;
        return menu;
    }

    private static ItemStack rareLife(int count) {
        ItemStack stack = new ItemStack(ModItems.LIFE_HERB_ITEM.get(), count);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(HerbGrade.RARE.modelId()));
        return stack;
    }

    @GameTest(template = "workbench_test")
    public static void slotsAndShiftClick(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuildAlchemyWorkbenchBlockEntity bench = place(helper, player);
        GuildAlchemyWorkbenchMenu menu = open(player, bench, 42);
        helper.assertTrue(menu.stillValid(player) && menu.slots.size() == 42, "Six workbench and 36 player slots must open");
        helper.assertTrue(menu.getSlot(0).mayPlace(rareLife(2)), "Herb slot accepts graded herbs");
        helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)), "Herb slot rejects stone");
        helper.assertTrue(menu.getSlot(3).mayPlace(new ItemStack(Items.REDSTONE)), "Catalyst slot accepts redstone");
        helper.assertFalse(menu.getSlot(3).mayPlace(new ItemStack(Items.DIRT)), "Catalyst slot rejects dirt");
        helper.assertTrue(menu.getSlot(4).mayPlace(new ItemStack(Items.GLASS_BOTTLE)), "Bottle slot accepts bottles");
        helper.assertFalse(menu.getSlot(5).mayPlace(new ItemStack(ModItems.LIFE_POTION.get())), "Output rejects insertion");
        menu.setCarried(new ItemStack(Items.DIAMOND));
        menu.clicked(5, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.DIAMOND) && !menu.getSlot(5).hasItem(), "Clicking output cannot insert an item");
        menu.setCarried(ItemStack.EMPTY);

        bench.inventory().setStackInSlot(0, rareLife(3));
        player.getInventory().setItem(9, rareLife(2));
        var chunk = helper.getLevel().getChunkAt(helper.absolutePos(ROOT));
        chunk.setUnsaved(false);
        helper.assertFalse(menu.quickMoveStack(player, 6).isEmpty(), "Shift-click moves a matching herb");
        helper.assertTrue(bench.inventory().getStackInSlot(0).getCount() == 5, "Matching herb stacks merge");
        helper.assertTrue(chunk.isUnsaved(), "In-place merge marks the workbench chunk for saving");
        CompoundTag saved = bench.saveWithoutMetadata(helper.getLevel().registryAccess());
        GuildAlchemyWorkbenchBlockEntity restored = new GuildAlchemyWorkbenchBlockEntity(helper.absolutePos(ROOT), helper.getBlockState(ROOT));
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.inventory().getStackInSlot(0).getCount() == 5
                && MoonlightHerbItem.gradeOf(restored.inventory().getStackInSlot(0)) == HerbGrade.RARE,
                "Merged counts and grade survive saving");

        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        helper.assertTrue(menu.quickMoveStack(player, 0).isEmpty(), "A full inventory cannot take a herb");
        helper.assertTrue(bench.inventory().getStackInSlot(0).getCount() == 5, "Failed shift-click cannot lose herbs");
        player.getInventory().setItem(9, ItemStack.EMPTY);
        helper.assertFalse(menu.quickMoveStack(player, 0).isEmpty(), "Shift-click returns herbs when space exists");
        helper.assertTrue(bench.inventory().getStackInSlot(0).isEmpty()
                && player.getInventory().getItem(9).getCount() == 5, "The exact herb count is transferred");
        menu.removed(player);
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void independentChoicesAndSharedTank(GameTestHelper helper) {
        Player alice = helper.makeMockPlayer(GameType.SURVIVAL);
        GuildAlchemyWorkbenchBlockEntity bench = place(helper, alice);
        Player bob = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos p = helper.absolutePos(ROOT);
        bob.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 1.5);
        GuildAlchemyWorkbenchMenu first = open(alice, bench, 1);
        GuildAlchemyWorkbenchMenu second = open(bob, bench, 2);
        helper.assertTrue(first.clickMenuButton(alice, 2) && first.clickMenuButton(alice, 13), "Valid selection buttons work");
        helper.assertTrue(first.selectedRecipe() == 2 && first.selectedGrade() == 3, "First menu stores choices");
        helper.assertTrue(second.selectedRecipe() == 0 && second.selectedGrade() == 0, "Choices are independent per viewer");
        helper.assertFalse(first.clickMenuButton(alice, 4), "Invalid recipe is rejected");
        helper.assertFalse(first.clickMenuButton(alice, 14), "Invalid grade is rejected");
        helper.assertFalse(first.clickMenuButton(alice, 21), "No crafting button is active yet");
        helper.assertFalse(first.clickMenuButton(bob, 20), "A different open menu cannot trigger this menu");
        alice.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
        bob.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(first.clickMenuButton(alice, 20), "First bucket fills tank");
        helper.assertTrue(second.clickMenuButton(bob, 20), "Second bucket fills same tank");
        helper.assertTrue(first.waterAmount() == 2000 && second.waterAmount() == 2000, "Both menus see shared water");
        helper.assertTrue(alice.getInventory().getItem(0).is(Items.BUCKET)
                && bob.getInventory().getItem(0).is(Items.BUCKET), "Both empty buckets are returned");
        alice.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(first.clickMenuButton(alice, 20), "Third bucket fits");
        alice.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(first.clickMenuButton(alice, 20), "Fourth bucket fits");
        bob.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
        helper.assertFalse(second.clickMenuButton(bob, 20), "A fifth bucket cannot overflow");
        helper.assertTrue(bob.getInventory().getItem(0).is(Items.WATER_BUCKET) && bench.waterAmount() == 4000,
                "Rejected bucket remains in inventory");
        bench.inventory().setStackInSlot(0, rareLife(3));
        helper.assertTrue(second.getSlot(0).getItem().getCount() == 3, "Viewers share the same ingredient storage");
        helper.getLevel().destroyBlock(helper.absolutePos(ROOT), true, alice);
        helper.assertFalse(first.stillValid(alice) || second.stillValid(bob), "Menus close after workbench is destroyed");
        helper.assertFalse(first.clickMenuButton(alice, 20), "A stale menu cannot consume a bucket");
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void recipePreviews(GameTestHelper helper) {
        Item[][] expected = {
                {ModItems.LIFE_HERB_ITEM.get(), ModItems.VITALITY_HERB_ITEM.get(), ModItems.POISON_MIST_HERB_ITEM.get()},
                {ModItems.FROST_HERB_ITEM.get(), ModItems.STAR_MOSS_ITEM.get(), ModItems.LIFE_HERB_ITEM.get()},
                {ModItems.SUN_HERB_ITEM.get(), ModItems.GALE_HERB_ITEM.get(), ModItems.VITALITY_HERB_ITEM.get()},
                {ModItems.MOONLIGHT_HERB_ITEM.get(), ModItems.FROST_HERB_ITEM.get(), ModItems.STAR_MOSS_ITEM.get()}
        };
        int[] counts = {3, 2, 1};
        Item[] catalysts = {Items.REDSTONE, Items.REDSTONE, Items.GLOWSTONE_DUST, Items.BLAZE_POWDER};
        int[] catalystCounts = {2, 3, 2, 2};
        Item[] potions = {ModItems.LIFE_POTION.get(), ModItems.GUARD_POTION.get(),
                ModItems.BATTLE_POTION.get(), ModItems.MOONLIGHT_POTION.get()};
        for (int recipe = 0; recipe < 4; recipe++) {
            for (HerbGrade grade : HerbGrade.values()) {
                for (int i = 0; i < 3; i++) {
                    ItemStack stack = GuildAlchemyPreview.herb(recipe, i, grade);
                    helper.assertTrue(stack.is(expected[recipe][i]) && stack.getCount() == counts[i]
                            && MoonlightHerbItem.gradeOf(stack) == grade, "Every preview must match herb, quantity and grade");
                }
                ItemStack catalyst = GuildAlchemyPreview.catalyst(grade);
                helper.assertTrue(catalyst.is(catalysts[grade.modelId()]) && catalyst.getCount() == catalystCounts[grade.modelId()],
                        "Every catalyst preview must match grade");
                helper.assertTrue(GuildAlchemyPreview.potion(recipe, grade).is(potions[recipe]), "Potion preview must match recipe");
                helper.assertTrue(GuildAlchemyPreview.ingredient(recipe, 4, grade).is(Items.GLASS_BOTTLE), "Empty bottle is separate");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void creativeWaterButton(GameTestHelper helper) {
        Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        GuildAlchemyWorkbenchBlockEntity bench = place(helper, creative);
        GuildAlchemyWorkbenchMenu menu = open(creative, bench, 3);
        helper.assertFalse(menu.clickMenuButton(creative, 20), "A bucket is required even in creative");
        creative.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
        for (int i = 1; i <= 4; i++) {
            helper.assertTrue(menu.clickMenuButton(creative, 20), "Creative bucket should fill");
            helper.assertTrue(bench.waterAmount() == i * 1000, "Each press fills one bucket");
            helper.assertTrue(creative.getInventory().getItem(0).is(Items.WATER_BUCKET), "Creative keeps the water bucket");
        }
        helper.assertFalse(menu.clickMenuButton(creative, 20), "Creative cannot overfill the tank");
        helper.assertTrue(creative.getInventory().countItem(Items.BUCKET) == 0, "Creative receives no duplicate empty bucket");
        helper.succeed();
    }
}
