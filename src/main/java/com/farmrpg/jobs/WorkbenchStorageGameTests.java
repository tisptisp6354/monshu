package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FarmRPGJobs.MODID)
@PrefixGameTestTemplate(false)
public final class WorkbenchStorageGameTests {
    private static final BlockPos SUPPORT = new BlockPos(1, 1, 1);
    private static final BlockPos ROOT = SUPPORT.above();
    private static final BlockPos[] PARTS = {ROOT, ROOT.east(), ROOT.above(), ROOT.east().above()};

    private static GuildAlchemyWorkbenchBlockEntity place(GameTestHelper helper, Player player) {
        helper.setBlock(SUPPORT, Blocks.STONE);
        player.setYRot(Direction.SOUTH.toYRot());
        ItemStack stack = new ItemStack(ModItems.GUILD_ALCHEMY_WORKBENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.placeAt(player, stack, SUPPORT, Direction.UP);
        helper.assertBlockPresent(ModBlocks.GUILD_ALCHEMY_WORKBENCH.get(), ROOT);
        return helper.getBlockEntity(ROOT);
    }

    private static ItemStack rareLife(int count) {
        ItemStack stack = new ItemStack(ModItems.LIFE_HERB_ITEM.get(), count);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(HerbGrade.RARE.modelId()));
        return stack;
    }

    @GameTest(template = "workbench_test")
    public static void bucketsCapacityAndSync(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuildAlchemyWorkbenchBlockEntity bench = place(helper, player);
        helper.assertTrue(bench.waterAmount() == 0, "A new tank must be empty");
        for (int i = 0; i < PARTS.length; i++) {
            if (i > 0) {
                helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(PARTS[i])) == null,
                        "Only the controller may have storage");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            helper.useBlock(PARTS[i], player);
            helper.assertTrue(bench.waterAmount() == (i + 1) * 1000, "One bucket must add 1000 mB");
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Survival gets an empty bucket");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.useBlock(ROOT, player);
        helper.assertTrue(bench.waterAmount() == 4000 && player.getMainHandItem().is(Items.WATER_BUCKET),
                "A full tank must not consume the bucket");
        helper.assertTrue(bench.drainWater(250, true) && bench.waterAmount() == 4000, "Simulated drain changes nothing");
        helper.assertTrue(bench.drainWater(250, false) && bench.waterAmount() == 3750, "Exact water drain must work");
        helper.assertFalse(bench.fillWaterBucket(), "A partial bucket must not fit");
        helper.assertFalse(bench.drainWater(4000, false), "Overdrawing must fail");
        helper.assertTrue(bench.drainWater(750, false) && bench.waterAmount() == 3000, "Tank must have room for a bucket");
        Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.useBlock(ROOT.east().above(), creative);
        helper.assertTrue(bench.waterAmount() == 4000 && creative.getMainHandItem().is(Items.WATER_BUCKET),
                "Creative should fill while keeping its water bucket");
        helper.assertTrue(creative.getInventory().countItem(Items.BUCKET) == 0,
                "Creative must not gain an extra empty bucket");

        HolderLookup.Provider regs = helper.getLevel().registryAccess();
        CompoundTag sync = bench.getUpdateTag(regs);
        helper.assertTrue(sync.getInt("WaterAmount") == 4000 && !sync.contains("Inventory"),
                "Only water state should be sent in the block update");
        GuildAlchemyWorkbenchBlockEntity remote = new GuildAlchemyWorkbenchBlockEntity(
                helper.absolutePos(ROOT), helper.getBlockState(ROOT));
        remote.handleUpdateTag(sync, regs);
        helper.assertTrue(remote.waterAmount() == 4000, "Client update should load the water amount");
        helper.assertTrue(bench.getUpdatePacket() != null, "The server must supply a block-entity packet");
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void inventoryAndDataRoundTrip(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuildAlchemyWorkbenchBlockEntity bench = place(helper, player);
        var inventory = bench.inventory();
        helper.assertTrue(inventory.getSlots() == 6, "Exactly six workbench slots are required");
        helper.assertTrue(inventory.insertItem(0, new ItemStack(Items.STONE), false).is(Items.STONE), "Wrong herbs must be rejected");
        helper.assertTrue(inventory.insertItem(0, rareLife(3), false).isEmpty(), "Herb slot accepts an existing graded herb");
        helper.assertTrue(inventory.insertItem(1, new ItemStack(ModItems.VITALITY_HERB_ITEM.get(), 2), false).isEmpty(), "Second herb slot must accept herbs");
        helper.assertTrue(inventory.insertItem(2, new ItemStack(ModItems.POISON_MIST_HERB_ITEM.get()), false).isEmpty(), "Third herb slot must accept herbs");
        helper.assertTrue(inventory.insertItem(3, new ItemStack(Items.GLOWSTONE_DUST, 2), false).isEmpty(), "Catalyst slot must accept catalyst");
        helper.assertTrue(inventory.insertItem(4, new ItemStack(Items.GLASS_BOTTLE, 5), false).isEmpty(), "Bottle slot must accept bottles");
        ItemStack potion = LifePotionItem.forGrade(ModItems.LIFE_POTION.get(), HerbGrade.RARE);
        potion.setCount(2);
        helper.assertTrue(inventory.insertItem(5, potion.copy(), false).getCount() == 2, "Output must reject insertion");
        inventory.setStackInSlot(5, potion.copy()); // Simulate a future server crafting result.
        helper.assertTrue(bench.fillWaterBucket() && bench.fillWaterBucket(), "Two buckets must fit");

        HolderLookup.Provider regs = helper.getLevel().registryAccess();
        CompoundTag saved = bench.saveWithoutMetadata(regs);
        GuildAlchemyWorkbenchBlockEntity restored = new GuildAlchemyWorkbenchBlockEntity(
                helper.absolutePos(ROOT), helper.getBlockState(ROOT));
        restored.loadWithComponents(saved, regs);
        helper.assertTrue(restored.waterAmount() == 2000, "Water must survive serialization");
        helper.assertTrue(restored.inventory().getSlots() == 6, "Six slots must survive serialization");
        helper.assertTrue(restored.inventory().getStackInSlot(0).getCount() == 3
                && MoonlightHerbItem.gradeOf(restored.inventory().getStackInSlot(0)) == HerbGrade.RARE,
                "Herb count and grade must survive serialization");
        helper.assertTrue(restored.inventory().getStackInSlot(5).getCount() == 2
                && MoonlightHerbItem.gradeOf(restored.inventory().getStackInSlot(5)) == HerbGrade.RARE,
                "Output count and grade must survive serialization");
        saved.getCompound("Inventory").putInt("Size", 99);
        restored.loadWithComponents(saved, regs);
        helper.assertTrue(restored.inventory().getSlots() == 6, "Malformed NBT cannot expand the slots");
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void breakAnyPartDropsContentsOnce(GameTestHelper helper) {
        var block = ModBlocks.GUILD_ALCHEMY_WORKBENCH.get();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (BlockPos broken : PARTS) {
            helper.killAllEntitiesOfClass(ItemEntity.class);
            GuildAlchemyWorkbenchBlockEntity bench = place(helper, player);
            bench.inventory().setStackInSlot(0, rareLife(3));
            bench.inventory().setStackInSlot(1, new ItemStack(ModItems.VITALITY_HERB_ITEM.get(), 2));
            bench.inventory().setStackInSlot(2, new ItemStack(ModItems.POISON_MIST_HERB_ITEM.get()));
            bench.inventory().setStackInSlot(3, new ItemStack(Items.GLOWSTONE_DUST, 2));
            bench.inventory().setStackInSlot(4, new ItemStack(Items.GLASS_BOTTLE, 5));
            ItemStack potion = LifePotionItem.forGrade(ModItems.LIFE_POTION.get(), HerbGrade.RARE);
            potion.setCount(2);
            bench.inventory().setStackInSlot(5, potion);
            helper.assertTrue(bench.fillWaterBucket(), "Water must enter before break");
            helper.assertTrue(helper.getLevel().destroyBlock(helper.absolutePos(broken), true, player), "Break must succeed");
            for (BlockPos part : PARTS) helper.assertBlockNotPresent(block, part);
            helper.assertItemEntityCountIs(ModItems.GUILD_ALCHEMY_WORKBENCH.get(), ROOT, 2.0, 1);
            helper.assertItemEntityCountIs(ModItems.LIFE_HERB_ITEM.get(), ROOT, 2.0, 3);
            helper.assertItemEntityCountIs(ModItems.VITALITY_HERB_ITEM.get(), ROOT, 2.0, 2);
            helper.assertItemEntityCountIs(ModItems.POISON_MIST_HERB_ITEM.get(), ROOT, 2.0, 1);
            helper.assertItemEntityCountIs(Items.GLOWSTONE_DUST, ROOT, 2.0, 2);
            helper.assertItemEntityCountIs(Items.GLASS_BOTTLE, ROOT, 2.0, 5);
            helper.assertItemEntityCountIs(ModItems.LIFE_POTION.get(), ROOT, 2.0, 2);
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(ROOT)).isEmpty(), "Stored water vanishes on break");
        }
        helper.succeed();
    }
}
