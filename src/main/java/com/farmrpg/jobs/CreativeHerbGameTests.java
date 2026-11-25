package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FarmRPGJobs.MODID)
@PrefixGameTestTemplate(false)
public final class CreativeHerbGameTests {
    private static final BlockPos SUPPORT = new BlockPos(1, 1, 1);
    private static final BlockPos ROOT = SUPPORT.above();

    private static CreativeModeTab ingredientTab(GameTestHelper helper) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.INGREDIENTS);
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.VANILLA_SET, true, helper.getLevel().registryAccess()));
        return tab;
    }

    private static Item[] herbs() {
        return new Item[] {ModItems.MOONLIGHT_HERB_ITEM.get(), ModItems.LIFE_HERB_ITEM.get(),
                ModItems.SUN_HERB_ITEM.get(), ModItems.FROST_HERB_ITEM.get(),
                ModItems.POISON_MIST_HERB_ITEM.get(), ModItems.VITALITY_HERB_ITEM.get(),
                ModItems.GALE_HERB_ITEM.get(), ModItems.STAR_MOSS_ITEM.get()};
    }

    private static ItemStack find(CreativeModeTab tab, Item item, HerbGrade grade) {
        return tab.getDisplayItems().stream().filter(stack -> stack.is(item) && MoonlightHerbItem.gradeOf(stack) == grade)
                .findFirst().orElseThrow();
    }

    @GameTest(template = "workbench_test")
    public static void allHerbGradesAppearInCreativeAndSearch(GameTestHelper helper) {
        CreativeModeTab tab = ingredientTab(helper);
        int verified = 0;
        for (Item item : herbs()) {
            for (HerbGrade grade : HerbGrade.values()) {
                long inParent = tab.getDisplayItems().stream()
                        .filter(stack -> stack.is(item) && MoonlightHerbItem.gradeOf(stack) == grade).count();
                long inSearch = tab.getSearchTabDisplayItems().stream()
                        .filter(stack -> stack.is(item) && MoonlightHerbItem.gradeOf(stack) == grade).count();
                helper.assertTrue(inParent == 1 && inSearch == 1, "Each item and grade appears once in both tab views");
                ItemStack stack = find(tab, item, grade);
                helper.assertTrue(stack.getCount() == 1, "Creative entries have exactly one item");
                var model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
                helper.assertTrue(grade == HerbGrade.COMMON ? model == null : model != null && model.value() == grade.modelId(),
                        "Creative entries use the existing harvest grade data");
                verified++;
            }
        }
        helper.assertTrue(verified == 32, "Eight herb types times four grades yields 32 entries");
        helper.succeed();
    }

    @GameTest(template = "workbench_test")
    public static void creativeHerbGradesCanCraft(GameTestHelper helper) {
        int[] oldChances = WorkbenchCraftingGameTests.guaranteeAllChances();
        try {
        CreativeModeTab tab = ingredientTab(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absolute = helper.absolutePos(ROOT);
        player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 1.5);
        helper.setBlock(SUPPORT, Blocks.STONE);
        player.setYRot(Direction.SOUTH.toYRot());
        ItemStack workbench = new ItemStack(ModItems.GUILD_ALCHEMY_WORKBENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, workbench);
        helper.placeAt(player, workbench, SUPPORT, Direction.UP);
        GuildAlchemyWorkbenchBlockEntity bench = helper.getBlockEntity(ROOT);
        helper.assertTrue(bench.fillWaterBucket(), "A water bucket fills the test workbench");
        var menu = new GuildAlchemyWorkbenchMenu(71, player.getInventory(), bench);
        player.containerMenu = menu;
        for (HerbGrade grade : HerbGrade.values()) {
            int recipeGrade = grade.modelId();
            helper.assertTrue(menu.clickMenuButton(player, GuildAlchemyWorkbenchMenu.GRADE_BUTTON_BASE + recipeGrade),
                    "Select a grade in the actual workbench menu");
            bench.inventory().setStackInSlot(0, find(tab, ModItems.LIFE_HERB_ITEM.get(), grade).copyWithCount(3));
            bench.inventory().setStackInSlot(1, find(tab, ModItems.VITALITY_HERB_ITEM.get(), grade).copyWithCount(2));
            bench.inventory().setStackInSlot(2, find(tab, ModItems.POISON_MIST_HERB_ITEM.get(), grade).copyWithCount(1));
            Item catalyst = switch (grade) {
                case COMMON, ADVANCED -> Items.REDSTONE;
                case RARE -> Items.GLOWSTONE_DUST;
                case HEROIC -> Items.BLAZE_POWDER;
            };
            bench.inventory().setStackInSlot(3, new ItemStack(catalyst, grade == HerbGrade.ADVANCED ? 3 : 2));
            bench.inventory().setStackInSlot(4, new ItemStack(Items.GLASS_BOTTLE));
            helper.assertTrue(menu.clickMenuButton(player, GuildAlchemyWorkbenchMenu.CRAFT_BUTTON),
                    "The real creative herb stacks work in their matching grade recipe");
            ItemStack result = bench.inventory().extractItem(5, 1, false);
            helper.assertTrue(result.is(ModItems.LIFE_POTION.get()) && MoonlightHerbItem.gradeOf(result) == grade,
                    "Crafted potion keeps the chosen grade");
        }
        helper.assertTrue(bench.waterAmount() == 0, "Four crafts consumed exactly one bucket of test water");
        } finally {
            WorkbenchCraftingGameTests.restoreChances(oldChances);
        }
        helper.succeed();
    }
}
