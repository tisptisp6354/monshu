package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FarmRPGJobs.MODID)
@PrefixGameTestTemplate(false)
public final class WorkbenchMultiBlockGameTests {
    private static final GuildAlchemyWorkbenchBlock.Part[] PARTS = {
            GuildAlchemyWorkbenchBlock.Part.LOWER_LEFT,
            GuildAlchemyWorkbenchBlock.Part.LOWER_RIGHT,
            GuildAlchemyWorkbenchBlock.Part.UPPER_LEFT,
            GuildAlchemyWorkbenchBlock.Part.UPPER_RIGHT
    };
    private static final Direction[] DIRECTIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static BlockPos position(BlockPos root, Direction facing, GuildAlchemyWorkbenchBlock.Part part) {
        return switch (part) {
            case LOWER_LEFT -> root;
            case LOWER_RIGHT -> root.relative(facing.getClockWise());
            case UPPER_LEFT -> root.above();
            case UPPER_RIGHT -> root.relative(facing.getClockWise()).above();
            case LEGACY -> throw new IllegalArgumentException("not an assembled part");
        };
    }

    @GameTest(template = "workbench_test")
    public static void allOrientationsAndBreakLocations(GameTestHelper helper) {
        var block = ModBlocks.GUILD_ALCHEMY_WORKBENCH.get();
        var item = ModItems.GUILD_ALCHEMY_WORKBENCH.get();
        BlockPos support = new BlockPos(1, 1, 1);
        BlockPos root = support.above();
        helper.setBlock(support, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        for (Direction facing : DIRECTIONS) {
            for (GuildAlchemyWorkbenchBlock.Part broken : PARTS) {
                helper.killAllEntitiesOfClass(ItemEntity.class);
                player.setYRot(facing.getOpposite().toYRot());
                ItemStack stack = new ItemStack(item, 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                helper.placeAt(player, stack, support, Direction.UP);
                helper.assertTrue(stack.getCount() == 1, "Placement must consume exactly one item");
                for (GuildAlchemyWorkbenchBlock.Part part : PARTS) {
                    BlockPos relative = position(root, facing, part);
                    helper.assertBlockPresent(block, relative);
                    BlockState actual = helper.getBlockState(relative);
                    helper.assertTrue(actual.getValue(GuildAlchemyWorkbenchBlock.PART) == part, "Incorrect piece");
                    helper.assertTrue(actual.getValue(GuildAlchemyWorkbenchBlock.FACING) == facing, "Incorrect facing");
                    helper.assertTrue(GuildAlchemyWorkbenchBlock.controllerPos(helper.absolutePos(relative), actual)
                            .equals(helper.absolutePos(root)), "All pieces must locate the same controller");
                    helper.useBlock(relative, player);
                    helper.assertTrue(stack.getCount() == 1, "Right-click must not consume the held item");
                }
                BlockPos target = position(root, facing, broken);
                BlockPos absolute = helper.absolutePos(target);
                BlockState state = helper.getLevel().getBlockState(absolute);
                state.getBlock().playerWillDestroy(helper.getLevel(), absolute, state, player);
                boolean removed = state.onDestroyedByPlayer(helper.getLevel(), absolute, player, true,
                        helper.getLevel().getFluidState(absolute));
                helper.assertTrue(removed, "Player must be able to break the selected piece");
                state.getBlock().playerDestroy(helper.getLevel(), player, absolute, state, null, ItemStack.EMPTY);
                for (GuildAlchemyWorkbenchBlock.Part part : PARTS) {
                    helper.assertBlockNotPresent(block, position(root, facing, part));
                }
                helper.assertItemEntityCountIs(item, root, 2.0, 1);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void obstructionCreativeAndLegacy(GameTestHelper helper) {
        var block = ModBlocks.GUILD_ALCHEMY_WORKBENCH.get();
        var item = ModItems.GUILD_ALCHEMY_WORKBENCH.get();
        BlockPos support = new BlockPos(1, 1, 1);
        BlockPos root = support.above();
        Direction facing = Direction.NORTH;
        BlockPos blocked = position(root, facing, GuildAlchemyWorkbenchBlock.Part.UPPER_RIGHT);
        helper.setBlock(support, Blocks.STONE);
        Player survival = helper.makeMockPlayer(GameType.SURVIVAL);
        survival.setYRot(facing.getOpposite().toYRot());
        ItemStack stack = new ItemStack(item, 2);
        survival.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.setBlock(blocked, Blocks.STONE);
        helper.placeAt(survival, stack, support, Direction.UP);
        helper.assertBlockNotPresent(block, root);
        helper.assertBlockPresent(Blocks.STONE, blocked);
        helper.assertTrue(stack.getCount() == 2, "Obstructed placement must not consume the item");
        helper.setBlock(blocked, Blocks.AIR);

        ItemStack modified = new ItemStack(item);
        modified.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
                .with(GuildAlchemyWorkbenchBlock.PART, GuildAlchemyWorkbenchBlock.Part.UPPER_RIGHT));
        survival.setItemInHand(InteractionHand.MAIN_HAND, modified);
        helper.placeAt(survival, modified, support, Direction.UP);
        helper.assertBlockNotPresent(block, root);
        helper.assertTrue(modified.getCount() == 1, "Invalid block-state data must not place or consume an item");

        survival.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.placeAt(survival, stack, support, Direction.UP);
        helper.assertBlockPresent(block, root);
        helper.killAllEntitiesOfClass(ItemEntity.class);
        Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        BlockPos target = position(root, facing, GuildAlchemyWorkbenchBlock.Part.UPPER_RIGHT);
        BlockPos absolute = helper.absolutePos(target);
        BlockState state = helper.getLevel().getBlockState(absolute);
        state.getBlock().playerWillDestroy(helper.getLevel(), absolute, state, creative);
        helper.assertTrue(state.onDestroyedByPlayer(helper.getLevel(), absolute, creative, false,
                helper.getLevel().getFluidState(absolute)), "Creative break must succeed");
        for (GuildAlchemyWorkbenchBlock.Part part : PARTS) {
            helper.assertBlockNotPresent(block, position(root, facing, part));
        }
        helper.assertItemEntityCountIs(item, root, 2.0, 0);

        CompoundTag oldState = new CompoundTag();
        oldState.putString("Name", "farmrpgjobs:guild_alchemy_workbench");
        CompoundTag properties = new CompoundTag();
        properties.putString("facing", "east");
        oldState.put("Properties", properties);
        BlockState legacy = NbtUtils.readBlockState(helper.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK), oldState);
        helper.assertTrue(legacy.is(block), "Old saved block ID must resolve");
        helper.assertTrue(legacy.getValue(GuildAlchemyWorkbenchBlock.FACING) == Direction.EAST,
                "Old facing must be retained");
        helper.assertTrue(legacy.getValue(GuildAlchemyWorkbenchBlock.PART) == GuildAlchemyWorkbenchBlock.Part.LEGACY,
                "Old state without part must use the legacy model");
        helper.setBlock(root, legacy);
        helper.assertTrue(helper.getLevel().destroyBlock(helper.absolutePos(root), true, survival), "Legacy block must be removable");
        helper.assertItemEntityCountIs(item, root, 2.0, 1);
        helper.succeed();
    }
}
