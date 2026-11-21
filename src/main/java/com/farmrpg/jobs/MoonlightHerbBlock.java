package com.farmrpg.jobs;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MoonlightHerbBlock extends BushBlock {
    public static final MapCodec<MoonlightHerbBlock> CODEC = simpleCodec(MoonlightHerbBlock::new);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 15.0, 13.0);

    public MoonlightHerbBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MoonlightHerbBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState ground, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(ground, level, pos)
                || ground.is(BlockTags.SAND)
                || ground.is(BlockTags.BASE_STONE_OVERWORLD)
                || ground.is(Blocks.SNOW_BLOCK)
                || ground.is(Blocks.MOSS_BLOCK);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)
                || !level.getBlockState(pos).is(this) || !level.removeBlock(pos, false)) {
            return InteractionResult.FAIL;
        }

        boolean herbalist = "herbalist".equals(JobData.life(serverPlayer));
        RandomSource random = serverLevel.getRandom();
        HerbGrade grade = HerbGrade.fromRoll(herbalist, random.nextInt(100));
        boolean bonus = herbalist && HerbGrade.grantsBonus(random.nextInt(100));
        Inventory inventory = player.getInventory();
        for (int i = 0; i < (bonus ? 2 : 1); i++) {
            ItemStack herb = new ItemStack(this.asItem());
            if (grade.modelId() != 0) {
                herb.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(grade.modelId()));
            }
            if (inventory.getSlotWithRemainingSpace(herb) >= 0 || inventory.getFreeSlot() >= 0) {
                inventory.add(herb);
            }
            if (!herb.isEmpty()) {
                player.drop(herb, false);
            }
        }
        GatheringEffects.playHerbHarvest(serverPlayer, serverLevel, pos);
        if (bonus) {
            GatheringEffects.playHerbalistBonus(serverPlayer, serverLevel, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }

        level.addParticle(
                random.nextBoolean() ? ParticleTypes.GLOW : ParticleTypes.END_ROD,
                pos.getX() + 0.2 + random.nextDouble() * 0.6,
                pos.getY() + 0.35 + random.nextDouble() * 0.7,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                0.0, 0.005, 0.0);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
