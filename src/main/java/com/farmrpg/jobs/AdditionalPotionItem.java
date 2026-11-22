package com.farmrpg.jobs;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/** Graded buff potions and an inactive item reserved for future mana integration. */
public final class AdditionalPotionItem extends PotionItem {
    public enum Kind { GUARD, BATTLE, MOONLIGHT }

    private final Kind kind;

    public AdditionalPotionItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public static ItemStack forGrade(AdditionalPotionItem potion, HerbGrade grade) {
        ItemStack stack = new ItemStack(potion);
        if (grade.modelId() != 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(grade.modelId()));
        }
        return stack;
    }

    @Override
    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getDescriptionId();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (kind == Kind.MOONLIGHT) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        // No mana system exists yet; never consume the potion without its effect.
        if (kind == Kind.MOONLIGHT || stack.isEmpty()) {
            return stack;
        }
        if (!level.isClientSide) {
            HerbGrade grade = MoonlightHerbItem.gradeOf(stack);
            if (kind == Kind.GUARD) {
                drinker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                        AlchemyPotionSettings.guardDurationTicks(grade),
                        AlchemyPotionSettings.guardAmplifier(grade)));
            } else {
                drinker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                        AlchemyPotionSettings.battleDurationTicks(grade),
                        AlchemyPotionSettings.battleAmplifier(grade)));
            }
        }
        // Keep vanilla drink events and avoid returning a glass bottle.
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
        if (kind == Kind.MOONLIGHT) {
            tooltip.add(Component.translatable("tooltip.farmrpgjobs.mana_unavailable")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
