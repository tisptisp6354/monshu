package com.farmrpg.jobs;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** First version: vanilla overworld ores only. No Fortune multiplication of bonus. */
public final class MinerPassive {
    private MinerPassive() {}
    private static final Map<Block, Item> REWARDS = Map.ofEntries(
        Map.entry(Blocks.COAL_ORE, Items.COAL), Map.entry(Blocks.DEEPSLATE_COAL_ORE, Items.COAL),
        Map.entry(Blocks.IRON_ORE, Items.RAW_IRON), Map.entry(Blocks.DEEPSLATE_IRON_ORE, Items.RAW_IRON),
        Map.entry(Blocks.COPPER_ORE, Items.RAW_COPPER), Map.entry(Blocks.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER),
        Map.entry(Blocks.GOLD_ORE, Items.RAW_GOLD), Map.entry(Blocks.DEEPSLATE_GOLD_ORE, Items.RAW_GOLD),
        Map.entry(Blocks.REDSTONE_ORE, Items.REDSTONE), Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, Items.REDSTONE),
        Map.entry(Blocks.LAPIS_ORE, Items.LAPIS_LAZULI), Map.entry(Blocks.DEEPSLATE_LAPIS_ORE, Items.LAPIS_LAZULI),
        Map.entry(Blocks.DIAMOND_ORE, Items.DIAMOND), Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, Items.DIAMOND),
        Map.entry(Blocks.EMERALD_ORE, Items.EMERALD), Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, Items.EMERALD));

    public static void onDrops(BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBreaker() instanceof ServerPlayer player)
                || player instanceof FakePlayer || player.isCreative() || !JobData.isMiner(player)) return;
        Item reward = REWARDS.get(event.getState().getBlock());
        if (reward == null) return;
        var silk = event.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH);
        if (EnchantmentHelper.getItemEnchantmentLevel(silk, event.getTool()) > 0) return;
        // Require the ordinary matching drop, so suppressed or incompatible loot does not yield a bonus.
        if (event.getDrops().stream().noneMatch(item -> item.getItem().is(reward))) return;
        if (event.getLevel().getRandom().nextFloat() < 0.30f) {
            var pos = event.getPos();
            event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, new ItemStack(reward, 1)));
            player.sendSystemMessage(Component.literal("[광부 패시브] ")
                    .append(Component.translatable(reward.getDescriptionId()))
                    .append(" 1개 추가 획득!"));
        }
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.isCanceled() && !event.getEntity().isCreative() && JobData.isMiner(event.getEntity())
                && REWARDS.containsKey(event.getState().getBlock())) {
            event.setNewSpeed(event.getNewSpeed() * 1.10f);
        }
    }
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) JobNetworking.syncLife(player);
    }
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) JobNetworking.syncLife(player);
    }
}
