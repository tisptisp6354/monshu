package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

/** Reusable feedback for gathering and successful gathering passives. */
public final class GatheringEffects {
    private static final DustParticleOptions GOLD_SPARKLE =
            new DustParticleOptions(new Vector3f(1.0f, 0.75f, 0.16f), 0.75f);
    private static final DustParticleOptions MOONLIGHT_SPARKLE =
            new DustParticleOptions(new Vector3f(0.2f, 0.9f, 1.0f), 0.9f);

    private GatheringEffects() {}

    public static void playSuccess(ServerPlayer player, ServerLevel level, BlockPos pos) {
        // Send only to the player who triggered the effect to avoid noise for nearby players.
        level.sendParticles(player, GOLD_SPARKLE, false,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.22, 0.22, 0.22, 0.01);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35f, 1.4f);
    }

    public static void playHerbHarvest(ServerPlayer player, ServerLevel level, BlockPos pos) {
        level.sendParticles(player, MOONLIGHT_SPARKLE, false,
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                10, 0.22, 0.22, 0.22, 0.01);
        level.sendParticles(player, ParticleTypes.END_ROD, false,
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                3, 0.16, 0.16, 0.16, 0.01);
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
