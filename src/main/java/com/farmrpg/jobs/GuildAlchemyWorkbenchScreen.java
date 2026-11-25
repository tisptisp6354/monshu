package com.farmrpg.jobs;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The existing illustrated background with real server-synchronized menu slots. */
public final class GuildAlchemyWorkbenchScreen extends AbstractContainerScreen<GuildAlchemyWorkbenchMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            FarmRPGJobs.MODID, "textures/gui/guild_alchemy_background.png");
    private static final int[] RECIPE_X = {222, 258, 222, 258};
    private static final int[] RECIPE_Y = {42, 42, 62, 62};
    private static final int[] GRADE_X = {222, 240, 258, 276};
    private static final int[] PREVIEW_X = {27, 53, 79, 105, 131};
    private static final int PREVIEW_Y = 109;
    private static final int GOLD = 0xFFFFD57A;
    private static final int WHITE = 0xFFFFF1D0;

    public GuildAlchemyWorkbenchScreen(GuildAlchemyWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GuildAlchemyWorkbenchMenu.WIDTH;
        imageHeight = GuildAlchemyWorkbenchMenu.HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, imageWidth, imageHeight, 0.0F, 0.0F,
                1536, 1024, 1536, 1024);

        // The illustration's tank is empty. Draw only the synchronized water amount over it.
        int height = Mth.clamp(menu.waterAmount(), 0, GuildAlchemyWorkbenchBlockEntity.WATER_CAPACITY) * 24
                / GuildAlchemyWorkbenchBlockEntity.WATER_CAPACITY;
        if (height > 0) {
            graphics.fill(leftPos + 186, topPos + 75 - height, leftPos + 201, topPos + 75, 0xC0009BE5);
            graphics.fill(leftPos + 188, topPos + 75 - height, leftPos + 190, topPos + 75, 0xA0C6F5FF);
        }

        // The illustration leaves room for a real 27-slot inventory and a nine-slot hotbar.
        for (int row = 0; row < 4; row++) {
            int y = row == 3 ? GuildAlchemyWorkbenchMenu.HOTBAR_Y : GuildAlchemyWorkbenchMenu.INVENTORY_Y + row * 18;
            for (int col = 0; col < 9; col++) {
                int x = GuildAlchemyWorkbenchMenu.INVENTORY_X + col * 18;
                graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF78513F);
                graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xE921141B);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 10, GOLD);
        graphics.drawCenteredString(font, Component.translatable("screen.farmrpgjobs.alchemy.inputs"), 68, 33, GOLD);
        graphics.drawCenteredString(font, Component.translatable("screen.farmrpgjobs.alchemy.main"), 40, 44, WHITE);
        graphics.drawCenteredString(font, Component.translatable("screen.farmrpgjobs.alchemy.support_a"), 68, 44, WHITE);
        graphics.drawCenteredString(font, Component.translatable("screen.farmrpgjobs.alchemy.support_b"), 95, 44, WHITE);
        graphics.drawCenteredString(font, Component.translatable("screen.farmrpgjobs.alchemy.catalyst"), 50, 71, WHITE);
        graphics.drawCenteredString(font, Component.translatable("screen.farmrpgjobs.alchemy.bottle"), 85, 71, WHITE);

        graphics.pose().pushPose();
        graphics.pose().translate(193, 79, 0);
        graphics.pose().scale(0.75F, 0.75F, 1.0F);
        graphics.drawCenteredString(font, Component.literal(menu.waterAmount() + " / 4000 mB"), 0, 0, WHITE);
        graphics.pose().popPose();
        graphics.drawCenteredString(font, Component.translatable("screen.farmrpgjobs.alchemy.fill"), 194, 89, WHITE);
        graphics.drawCenteredString(font, Component.translatable("screen.farmrpgjobs.alchemy.recipes"), 257, 32, GOLD);
        HerbGrade grade = HerbGrade.fromModelId(menu.selectedGrade());
        for (int i = 0; i < GuildAlchemyPreview.RECIPE_COUNT; i++) {
            if (menu.selectedRecipe() == i) graphics.renderOutline(RECIPE_X[i], RECIPE_Y[i], 34, 18, GOLD);
            ItemStack potion = GuildAlchemyPreview.potion(i, grade);
            graphics.renderItem(potion, RECIPE_X[i] + 1, RECIPE_Y[i] + 1);
            graphics.drawString(font, Component.translatable("screen.farmrpgjobs.alchemy.recipe." + i),
                    RECIPE_X[i] + 17, RECIPE_Y[i] + 5, WHITE, false);
        }
        for (int i = 0; i < HerbGrade.values().length; i++) {
            if (menu.selectedGrade() == i) graphics.renderOutline(GRADE_X[i], 87, 16, 10, GOLD);
            graphics.drawCenteredString(font, Component.translatable("screen.farmrpgjobs.alchemy.grade." + i),
                    GRADE_X[i] + 8, 88, WHITE);
        }

        graphics.drawString(font, Component.translatable("screen.farmrpgjobs.alchemy.required"), 25, 99, GOLD, false);
        graphics.drawString(font, Component.translatable("screen.farmrpgjobs.alchemy.output"), 188, 99, GOLD, false);
        for (int i = 0; i < PREVIEW_X.length; i++) {
            ItemStack ingredient = GuildAlchemyPreview.ingredient(menu.selectedRecipe(), i, grade);
            int valid = validIngredientCount(i, ingredient, grade);
            int color = valid >= ingredient.getCount() ? 0xFF62DD78 : 0xFFCF6559;
            graphics.renderOutline(PREVIEW_X[i] - 1, PREVIEW_Y - 1, 18, 18, color);
            graphics.renderItem(ingredient, PREVIEW_X[i], PREVIEW_Y);
            graphics.renderItemDecorations(font, ingredient, PREVIEW_X[i], PREVIEW_Y,
                    Integer.toString(ingredient.getCount()));
        }
        graphics.drawCenteredString(font, Component.translatable("screen.farmrpgjobs.alchemy.coming_soon"), 258, 113, WHITE);
    }

    private int validIngredientCount(int index, ItemStack expected, HerbGrade grade) {
        ItemStack actual = menu.slots.get(index).getItem();
        if (!actual.is(expected.getItem())) return 0;
        if (index < 3 && MoonlightHerbItem.gradeOf(actual) != grade) return 0;
        return actual.getCount();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty() || hoveredSlot != null) return;
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        HerbGrade grade = HerbGrade.fromModelId(menu.selectedGrade());
        for (int i = 0; i < PREVIEW_X.length; i++) {
            if (inside(x, y, PREVIEW_X[i], PREVIEW_Y, 16, 16)) {
                ItemStack expected = GuildAlchemyPreview.ingredient(menu.selectedRecipe(), i, grade);
                int have = validIngredientCount(i, expected, grade);
                List<Component> tooltip = new ArrayList<>(getTooltipFromItem(minecraft, expected));
                tooltip.add(Component.translatable("screen.farmrpgjobs.alchemy.ingredient_progress", have, expected.getCount())
                        .withStyle(have >= expected.getCount() ? ChatFormatting.GREEN : ChatFormatting.RED));
                graphics.renderTooltip(font, tooltip, expected.getTooltipImage(), expected, mouseX, mouseY);
                return;
            }
        }
        for (int i = 0; i < GuildAlchemyPreview.RECIPE_COUNT; i++) {
            if (inside(x, y, RECIPE_X[i], RECIPE_Y[i], 34, 18)) {
                graphics.renderTooltip(font, GuildAlchemyPreview.potion(i, grade), mouseX, mouseY);
                return;
            }
        }
        if (inside(x, y, 176, 87, 37, 11)) {
            graphics.renderTooltip(font, Component.translatable("screen.farmrpgjobs.alchemy.fill_tooltip"), mouseX, mouseY);
        } else if (inside(x, y, 224, 106, 68, 19)) {
            graphics.renderTooltip(font, Component.translatable("screen.farmrpgjobs.alchemy.craft_tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.getCarried().isEmpty() && minecraft != null && minecraft.gameMode != null) {
            double x = mouseX - leftPos;
            double y = mouseY - topPos;
            for (int i = 0; i < GuildAlchemyPreview.RECIPE_COUNT; i++) {
                if (inside(x, y, RECIPE_X[i], RECIPE_Y[i], 34, 18)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GuildAlchemyWorkbenchMenu.RECIPE_BUTTON_BASE + i);
                    return true;
                }
            }
            for (int i = 0; i < HerbGrade.values().length; i++) {
                if (inside(x, y, GRADE_X[i], 87, 16, 10)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GuildAlchemyWorkbenchMenu.GRADE_BUTTON_BASE + i);
                    return true;
                }
            }
            if (inside(x, y, 176, 87, 37, 11)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GuildAlchemyWorkbenchMenu.FILL_WATER_BUTTON);
                return true;
            }
            if (inside(x, y, 224, 106, 68, 19)) return true; // Crafting is deliberately inactive.
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
