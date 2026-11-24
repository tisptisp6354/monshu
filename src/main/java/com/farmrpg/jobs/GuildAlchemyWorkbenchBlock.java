package com.farmrpg.jobs;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Stage-two block; the alchemy menu and persistent storage will be added later. */
public final class GuildAlchemyWorkbenchBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<GuildAlchemyWorkbenchBlock> CODEC =
            simpleCodec(GuildAlchemyWorkbenchBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final Part[] ASSEMBLED_PARTS = {
            Part.LOWER_LEFT, Part.LOWER_RIGHT, Part.UPPER_LEFT, Part.UPPER_RIGHT
    };

    public enum Part implements StringRepresentable {
        LEGACY("legacy", 0, 0),
        LOWER_LEFT("lower_left", 0, 0),
        LOWER_RIGHT("lower_right", 1, 0),
        UPPER_LEFT("upper_left", 0, 1),
        UPPER_RIGHT("upper_right", 1, 1);

        private final String name;
        private final int column;
        private final int height;

        Part(String name, int column, int height) {
            this.name = name;
            this.column = column;
            this.height = height;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public GuildAlchemyWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Existing worlds have no "part" property: their workbenches stay single-block.
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.LEGACY));
    }

    @Override
    public MapCodec<GuildAlchemyWorkbenchBlock> codec() {
        return CODEC;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos root = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState rootState = defaultBlockState().setValue(FACING, facing).setValue(PART, Part.LOWER_LEFT);
        CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);

        for (Part part : ASSEMBLED_PARTS) {
            BlockPos partPos = positionFor(root, facing, part);
            if (!level.isInWorldBounds(partPos) || !level.getWorldBorder().isWithinBounds(partPos)
                    || !level.getBlockState(partPos).isAir()
                    || !level.isUnobstructed(rootState.setValue(PART, part), partPos, collision)
                    || (player != null && (!level.mayInteract(player, partPos)
                            || !player.mayUseItemAt(partPos, context.getClickedFace(), context.getItemInHand())))) {
                return null;
            }
        }
        return rootState;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    /** The lower-left block is the future owner of the workbench's saved inventory. */
    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        Part part = state.getValue(PART);
        if (part == Part.LEGACY) {
            return pos;
        }
        return pos.relative(state.getValue(FACING).getClockWise(), -part.column).below(part.height);
    }

    private static BlockPos positionFor(BlockPos root, Direction facing, Part part) {
        return root.relative(facing.getClockWise(), part.column).above(part.height);
    }

    /** Place the three remaining blocks after the item successfully places the controller. */
    boolean assemble(Level level, BlockPos root, BlockState rootState) {
        if (!level.getBlockState(root).equals(rootState)) {
            return false;
        }
        Direction facing = rootState.getValue(FACING);
        for (Part part : ASSEMBLED_PARTS) {
            if (part == Part.LOWER_LEFT) {
                continue;
            }
            BlockPos partPos = positionFor(root, facing, part);
            if (!level.getBlockState(partPos).isAir()
                    || !level.setBlock(partPos, rootState.setValue(PART, part), Block.UPDATE_ALL)
                    || !level.getBlockState(root).equals(rootState)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (state.is(newState.getBlock()) || state.getValue(PART) == Part.LEGACY) {
            return;
        }
        Direction facing = state.getValue(FACING);
        BlockPos root = controllerPos(pos, state);
        for (Part part : ASSEMBLED_PARTS) {
            BlockPos partPos = positionFor(root, facing, part);
            if (partPos.equals(pos)) {
                continue;
            }
            BlockState other = level.getBlockState(partPos);
            if (other.is(this) && other.getValue(FACING) == facing && other.getValue(PART) == part) {
                level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.farmrpgjobs.alchemy_not_ready"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
