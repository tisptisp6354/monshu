package com.farmrpg.jobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, FarmRPGJobs.MODID);

    static {
        FEATURES.register("herb_surface_patch", () -> new HerbPatchFeature(false));
        FEATURES.register("herb_cave_patch", () -> new HerbPatchFeature(true));
    }

    private ModFeatures() {}
}
