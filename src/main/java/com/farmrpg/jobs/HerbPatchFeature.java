package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

/** Attempts to place up to five herbs of this type inside the current chunk. */
public final class HerbPatchFeature extends Feature<SimpleBlockConfiguration> {
    private static final int TARGET_PER_CHUNK = 5;
    private static final int SURFACE_ATTEMPTS = 256;
    private static final int CAVE_ATTEMPTS = 256;
    private static final int MAX_SURFACE_SCAN = 24;
    private static final int MAX_CAVE_SCAN = 12;
    private static final int MIN_DEPTH_BELOW_SURFACE = 13;

    private final boolean underground;

    public HerbPatchFeature(boolean underground) {
        super(SimpleBlockConfiguration.CODEC);
        this.underground = underground;
    }

    @Override
    public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState herb = context.config().toPlace().getState(random, origin);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(herb.getBlock());
        TagKey<Biome> allowedBiomes = TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(FarmRPGJobs.MODID, blockId.getPath() + "_spawn"));
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;
        int placed = 0;
        int maxAttempts = underground ? CAVE_ATTEMPTS : SURFACE_ATTEMPTS;

        for (int attempt = 0; attempt < maxAttempts && placed < TARGET_PER_CHUNK; attempt++) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);
            BlockPos candidate = underground
                    ? findCaveFloor(level, random, x, z)
                    : findSurface(level, herb, x, z);
            if (candidate == null || !level.ensureCanWrite(candidate)
                    || !level.isEmptyBlock(candidate) || !level.getBiome(candidate).is(allowedBiomes)
                    || !herb.canSurvive(level, candidate)) {
                continue;
            }
            if (level.setBlock(candidate, herb, 2)) {
                placed++;
            }
        }
        return placed > 0;
    }

    private static BlockPos findSurface(WorldGenLevel level, BlockState herb, int x, int z) {
        int height = Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z),
                level.getMaxBuildHeight() - 1);
        int minHeight = level.getMinBuildHeight();
        for (int step = 0; step < MAX_SURFACE_SCAN && height > minHeight; step++, height--) {
            BlockPos pos = new BlockPos(x, height, z);
            BlockState current = level.getBlockState(pos);
            if (current.isAir()) {
                if (herb.canSurvive(level, pos)) {
                    return pos;
                }
            } else if (!current.is(BlockTags.LEAVES)) {
                return null;
            }
        }
        return null;
    }

    private static BlockPos findCaveFloor(WorldGenLevel level, RandomSource random, int x, int z) {
        int minHeight = level.getMinBuildHeight() + 1;
        int maxHeight = Math.min(level.getMaxBuildHeight() - 1,
                level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - MIN_DEPTH_BELOW_SURFACE);
        if (maxHeight < minHeight) {
            return null;
        }
        int y = minHeight + random.nextInt(maxHeight - minHeight + 1);
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.isEmptyBlock(pos)) {
            return null;
        }
        for (int step = 0; step < MAX_CAVE_SCAN && pos.getY() > minHeight; step++) {
            BlockPos below = pos.below();
            if (!level.isEmptyBlock(below)) {
                return pos;
            }
            pos = below;
        }
        return null;
    }
}
