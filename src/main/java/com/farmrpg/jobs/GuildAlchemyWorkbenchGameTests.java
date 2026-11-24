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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FarmRPGJobs.MODID)
@PrefixGameTestTemplate(false)
public final class GuildAlchemyWorkbenchGameTests {
    @GameTest(template = "workbench_test")
    public static void workbenchPlacementInteractionAndDrop(GameTestHelper helper) {
        var block = ModBlocks.GUILD_ALCHEMY_WORKBENCH.get();
        var item = ModItems.GUILD_ALCHEMY_WORKBENCH.get();
        helper.assertTrue(item.getBlock() == block && block.asItem() == item,
                "The registered item must place the registered workbench");
        BlockState north = block.defaultBlockState().setValue(GuildAlchemyWorkbenchBlock.FACING, Direction.NORTH);
        helper.assertTrue(north.rotate(Rotation.CLOCKWISE_90).getValue(GuildAlchemyWorkbenchBlock.FACING) == Direction.EAST,
                "Clockwise rotation must rotate the facing property");
        helper.assertTrue(north.rotate(Rotation.CLOCKWISE_180).getValue(GuildAlchemyWorkbenchBlock.FACING) == Direction.SOUTH,
                "Half rotation must rotate the facing property");

        BlockPos support = new BlockPos(1, 1, 1);
        BlockPos target = support.above();
        helper.setBlock(support, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(item, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.placeAt(player, stack, support, Direction.UP);
        helper.assertBlockPresent(block, target);
        helper.assertTrue(stack.getCount() == 1, "Survival placement must consume exactly one workbench");

        helper.useBlock(target, player);
        helper.assertBlockPresent(block, target);
        helper.assertTrue(stack.getCount() == 1, "Right-click interaction must not consume or place the held item");

        helper.assertTrue(helper.getLevel().destroyBlock(helper.absolutePos(target), true, player),
                "The workbench must be breakable");
        helper.assertBlockNotPresent(block, target);
        helper.assertItemEntityCountIs(item, target, 1.5, 1);
        helper.succeed();
    }
}
