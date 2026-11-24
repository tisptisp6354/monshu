package com.farmrpg.jobs;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Four-part workbench; only the controller stores items and water. */
public final class GuildAlchemyWorkbenchBlock extends HorizontalDirectionalBlock implements EntityBlock {
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

    private static boolean isController(BlockState state) {
        Part part = state.getValue(PART);
        return part == Part.LOWER_LEFT || part == Part.LEGACY;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isController(state) ? new GuildAlchemyWorkbenchBlockEntity(pos, state) : null;
    }

    /** The lower-left block is the future owner of the workbench's saved inventory. */
    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        Part part = state.getValue(PART);
        if (part == Part.LEGACY) {
            return pos;
        }
        return pos.relative(state.getValue(FACING).getClockWise(), -part.column).below(part.height);
    }

    @Nullable
    private GuildAlchemyWorkbenchBlockEntity workbenchAt(Level level, BlockPos pos, BlockState state) {
        BlockPos root = controllerPos(pos, state);
        BlockState rootState = level.getBlockState(root);
        if (!rootState.is(this) || !isController(rootState)
                || rootState.getValue(FACING) != state.getValue(FACING)) {
            return null;
        }
        BlockEntity entity = level.getBlockEntity(root);
        return entity instanceof GuildAlchemyWorkbenchBlockEntity workbench ? workbench : null;
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
        boolean sameBlock = state.is(newState.getBlock());
        boolean controllerRemoved = isController(state) && (!sameBlock || !isController(newState));
        if (controllerRemoved) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof GuildAlchemyWorkbenchBlockEntity workbench) {
                workbench.dropInventory();
            }
            // A change between parts of the same block also needs to release controller data.
            if (sameBlock) {
                level.removeBlockEntity(pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (sameBlock || state.getValue(PART) == Part.LEGACY) {
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.WATER_BUCKET)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        GuildAlchemyWorkbenchBlockEntity workbench = workbenchAt(level, pos, state);
        if (workbench == null) {
            player.displayClientMessage(Component.translatable("message.farmrpgjobs.alchemy_unavailable"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (!workbench.fillWaterBucket()) {
            player.displayClientMessage(Component.translatable("message.farmrpgjobs.alchemy_water_full",
                    workbench.waterAmount(), GuildAlchemyWorkbenchBlockEntity.WATER_CAPACITY), true);
            return ItemInteractionResult.CONSUME;
        }
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                BucketItem.getEmptySuccessItem(stack, player)));
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.farmrpgjobs.alchemy_water_filled",
                workbench.waterAmount(), GuildAlchemyWorkbenchBlockEntity.WATER_CAPACITY), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            GuildAlchemyWorkbenchBlockEntity workbench = workbenchAt(level, pos, state);
            if (workbench == null) {
                player.displayClientMessage(Component.translatable("message.farmrpgjobs.alchemy_unavailable"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.farmrpgjobs.alchemy_water_status",
                        workbench.waterAmount(), GuildAlchemyWorkbenchBlockEntity.WATER_CAPACITY), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
