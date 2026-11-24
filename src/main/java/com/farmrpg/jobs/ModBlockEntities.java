package com.farmrpg.jobs;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FarmRPGJobs.MODID);

    public static final Supplier<BlockEntityType<GuildAlchemyWorkbenchBlockEntity>> GUILD_ALCHEMY_WORKBENCH =
            TYPES.register("guild_alchemy_workbench", () -> BlockEntityType.Builder.of(
                    GuildAlchemyWorkbenchBlockEntity::new, ModBlocks.GUILD_ALCHEMY_WORKBENCH.get()).build(null));

    private ModBlockEntities() {}
}
