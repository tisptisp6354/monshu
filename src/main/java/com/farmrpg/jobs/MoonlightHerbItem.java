package com.farmrpg.jobs;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public class MoonlightHerbItem extends BlockItem {
    public MoonlightHerbItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        // Harvested herbs are ingredients and cannot be replanted.
        return InteractionResult.FAIL;
    }

    public static HerbGrade gradeOf(ItemStack stack) {
        CustomModelData data = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT);
        return HerbGrade.fromModelId(data.value());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        HerbGrade grade = gradeOf(stack);
        ChatFormatting color = switch (grade) {
            case COMMON -> ChatFormatting.WHITE;
            case ADVANCED -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.AQUA;
            case HEROIC -> ChatFormatting.LIGHT_PURPLE;
        };
        Component gradeName = Component.translatable("tooltip.farmrpgjobs.herb_grade." + grade.translationKey()).withStyle(color);
        tooltip.add(Component.translatable("tooltip.farmrpgjobs.herb_grade", gradeName).withStyle(ChatFormatting.GRAY));
    }
}
