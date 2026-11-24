package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Places all four parts as one item operation, or rolls back if assembly fails. */
public final class GuildAlchemyWorkbenchItem extends BlockItem {
    public GuildAlchemyWorkbenchItem(GuildAlchemyWorkbenchBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        // BlockItem applies these components after placement; changing facing/part would orphan pieces.
        if (!context.getItemInHand().getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).isEmpty()) {
            return false;
        }
        Level level = context.getLevel();
        BlockPos root = context.getClickedPos();
        BlockState previous = level.getBlockState(root);
        if (!super.placeBlock(context, state)) {
            return false;
        }
        if (((GuildAlchemyWorkbenchBlock) getBlock()).assemble(level, root, state)) {
            return true;
        }
        if (level.getBlockState(root).equals(state)) {
            level.setBlock(root, previous, Block.UPDATE_ALL);
        }
        return false;
    }
}
