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
    public static final DeferredBlock<MoonlightHerbBlock> LIFE_HERB = registerHerb("life_herb");
    public static final DeferredBlock<MoonlightHerbBlock> SUN_HERB = registerHerb("sun_herb");
    public static final DeferredBlock<MoonlightHerbBlock> FROST_HERB = registerHerb("frost_herb");
    public static final DeferredBlock<MoonlightHerbBlock> POISON_MIST_HERB = registerHerb("poison_mist_herb");
    public static final DeferredBlock<MoonlightHerbBlock> VITALITY_HERB = registerHerb("vitality_herb");
    public static final DeferredBlock<MoonlightHerbBlock> GALE_HERB = registerHerb("gale_herb");
    public static final DeferredBlock<MoonlightHerbBlock> STAR_MOSS = registerHerb("star_moss");
    public static final DeferredBlock<GuildAlchemyWorkbenchBlock> GUILD_ALCHEMY_WORKBENCH = BLOCKS.register(
            "guild_alchemy_workbench",
            () -> new GuildAlchemyWorkbenchBlock(BlockBehaviour.Properties.of()
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    private static DeferredBlock<MoonlightHerbBlock> registerHerb(String name) {
        return BLOCKS.register(name, () -> new MoonlightHerbBlock(BlockBehaviour.Properties.of()
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> 3)));
    }

    private ModBlocks() {}
}
