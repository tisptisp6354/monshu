package com.farmrpg.jobs;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/** A drinkable potion that stores its grade in the existing model-data format. */
public final class LifePotionItem extends PotionItem {
    public LifePotionItem(Properties properties) {
        super(properties);
    }

    public static ItemStack forGrade(LifePotionItem potion, HerbGrade grade) {
        ItemStack stack = new ItemStack(potion);
        if (grade.modelId() != 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(grade.modelId()));
        }
        return stack;
    }

    @Override
    public ItemStack getDefaultInstance() {
        // PotionItem's default is a water potion; this item has its own effect.
        return new ItemStack(this);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getDescriptionId();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        if (stack.isEmpty()) {
            return stack;
        }
        if (!level.isClientSide) {
            HerbGrade grade = MoonlightHerbItem.gradeOf(stack);
            drinker.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    AlchemyPotionSettings.lifeDurationTicks(grade),
                    AlchemyPotionSettings.lifeAmplifier(grade)));
        }
        // Delegate vanilla effects, statistics and drink events for one potion,
        // then consume it without returning the empty glass bottle.
        ItemStack result = super.finishUsingItem(stack.copyWithCount(1), level, drinker);
        if (result.is(Items.GLASS_BOTTLE)) {
            stack.consume(1, drinker);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        HerbGrade grade = MoonlightHerbItem.gradeOf(stack);
        ChatFormatting color = switch (grade) {
            case COMMON -> ChatFormatting.WHITE;
            case ADVANCED -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.AQUA;
            case HEROIC -> ChatFormatting.LIGHT_PURPLE;
        };
        Component name = Component.translatable("tooltip.farmrpgjobs.herb_grade." + grade.translationKey())
                .withStyle(color);
        tooltip.add(Component.translatable("tooltip.farmrpgjobs.potion_grade", name)
                .withStyle(ChatFormatting.GRAY));
    }
}
