package com.farmrpg.jobs;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FarmRPGJobs.MODID);
    public static final DeferredBlock<MoonlightHerbBlock> MOONLIGHT_HERB = BLOCKS.register(
            "moonlight_herb",
            () -> new MoonlightHerbBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .lightLevel(state -> 3)));

    private ModBlocks() {}
}
