package com.farmrpg.jobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FarmRPGJobs.MODID);
    public static final DeferredItem<MoonlightHerbItem> MOONLIGHT_HERB_ITEM =
            ITEMS.register("moonlight_herb_item", () -> new MoonlightHerbItem(ModBlocks.MOONLIGHT_HERB.get(), new Item.Properties()));

    static {
        // Resolve the former placeable-item ID to the single herb item when loading older saves.
        ITEMS.addAlias(
                ResourceLocation.fromNamespaceAndPath(FarmRPGJobs.MODID, "moonlight_herb"),
                ResourceLocation.fromNamespaceAndPath(FarmRPGJobs.MODID, "moonlight_herb_item"));
    }

    private ModItems() {}

    public static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(MOONLIGHT_HERB_ITEM.get());
        }
    }
}
