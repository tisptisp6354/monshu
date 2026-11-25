package com.farmrpg.jobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, FarmRPGJobs.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<GuildAlchemyWorkbenchMenu>> GUILD_ALCHEMY =
            MENUS.register("guild_alchemy_workbench",
                    () -> new MenuType<>(GuildAlchemyWorkbenchMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {}
}
