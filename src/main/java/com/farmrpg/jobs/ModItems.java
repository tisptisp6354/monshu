package com.farmrpg.jobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FarmRPGJobs.MODID);
    public static final DeferredItem<MoonlightHerbItem> MOONLIGHT_HERB_ITEM =
            ITEMS.register("moonlight_herb_item", () -> new MoonlightHerbItem(ModBlocks.MOONLIGHT_HERB.get(), new Item.Properties()));
    public static final DeferredItem<MoonlightHerbItem> LIFE_HERB_ITEM = registerHerb("life_herb_item", ModBlocks.LIFE_HERB);
    public static final DeferredItem<MoonlightHerbItem> SUN_HERB_ITEM = registerHerb("sun_herb_item", ModBlocks.SUN_HERB);
    public static final DeferredItem<MoonlightHerbItem> FROST_HERB_ITEM = registerHerb("frost_herb_item", ModBlocks.FROST_HERB);
    public static final DeferredItem<MoonlightHerbItem> POISON_MIST_HERB_ITEM = registerHerb("poison_mist_herb_item", ModBlocks.POISON_MIST_HERB);
    public static final DeferredItem<MoonlightHerbItem> VITALITY_HERB_ITEM = registerHerb("vitality_herb_item", ModBlocks.VITALITY_HERB);
    public static final DeferredItem<MoonlightHerbItem> GALE_HERB_ITEM = registerHerb("gale_herb_item", ModBlocks.GALE_HERB);
    public static final DeferredItem<MoonlightHerbItem> STAR_MOSS_ITEM = registerHerb("star_moss_item", ModBlocks.STAR_MOSS);
    public static final DeferredItem<LifePotionItem> LIFE_POTION =
            ITEMS.register("life_potion", () -> new LifePotionItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<AdditionalPotionItem> GUARD_POTION = ITEMS.register("guard_potion",
            () -> new AdditionalPotionItem(new Item.Properties().stacksTo(64), AdditionalPotionItem.Kind.GUARD));
    public static final DeferredItem<AdditionalPotionItem> BATTLE_POTION = ITEMS.register("battle_potion",
            () -> new AdditionalPotionItem(new Item.Properties().stacksTo(64), AdditionalPotionItem.Kind.BATTLE));
    public static final DeferredItem<AdditionalPotionItem> MOONLIGHT_POTION = ITEMS.register("moonlight_potion",
            () -> new AdditionalPotionItem(new Item.Properties().stacksTo(64), AdditionalPotionItem.Kind.MOONLIGHT));

    private static DeferredItem<MoonlightHerbItem> registerHerb(String name, DeferredBlock<MoonlightHerbBlock> block) {
        return ITEMS.register(name, () -> new MoonlightHerbItem(block.get(), new Item.Properties()));
    }

    static {
        // Resolve the former placeable-item ID to the single herb item when loading older saves.
        ITEMS.addAlias(
                ResourceLocation.fromNamespaceAndPath(FarmRPGJobs.MODID, "moonlight_herb"),
                ResourceLocation.fromNamespaceAndPath(FarmRPGJobs.MODID, "moonlight_herb_item"));
    }

    private ModItems() {}

    public static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FOOD_AND_DRINKS)) {
            for (HerbGrade grade : HerbGrade.values()) {
                event.accept(LifePotionItem.forGrade(LIFE_POTION.get(), grade));
            }
            for (HerbGrade grade : HerbGrade.values()) {
                event.accept(AdditionalPotionItem.forGrade(GUARD_POTION.get(), grade));
                event.accept(AdditionalPotionItem.forGrade(BATTLE_POTION.get(), grade));
                event.accept(AdditionalPotionItem.forGrade(MOONLIGHT_POTION.get(), grade));
            }
        }
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(MOONLIGHT_HERB_ITEM.get());
            event.accept(LIFE_HERB_ITEM.get());
            event.accept(SUN_HERB_ITEM.get());
            event.accept(FROST_HERB_ITEM.get());
            event.accept(POISON_MIST_HERB_ITEM.get());
            event.accept(VITALITY_HERB_ITEM.get());
            event.accept(GALE_HERB_ITEM.get());
            event.accept(STAR_MOSS_ITEM.get());
        }
    }
}
