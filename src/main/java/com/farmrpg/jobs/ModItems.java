package com.farmrpg.jobs;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FarmRPGJobs.MODID);
    public static final DeferredItem<Item> MOONLIGHT_HERB_ITEM = ITEMS.registerSimpleItem("moonlight_herb_item");
    public static final DeferredItem<BlockItem> MOONLIGHT_HERB_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("moonlight_herb", ModBlocks.MOONLIGHT_HERB);

    private ModItems() {}

    public static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(MOONLIGHT_HERB_ITEM.get());
            event.accept(MOONLIGHT_HERB_BLOCK_ITEM.get());
        }
    }
}
