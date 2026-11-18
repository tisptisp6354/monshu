package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

/** Reusable feedback for successful gathering passives. */
public final class GatheringEffects {
    private static final DustParticleOptions GOLD_SPARKLE =
            new DustParticleOptions(new Vector3f(1.0f, 0.75f, 0.16f), 0.75f);

    private GatheringEffects() {}

    public static void playSuccess(ServerPlayer player, ServerLevel level, BlockPos pos) {
        // Send only to the player who triggered the effect to avoid noise for nearby players.
        level.sendParticles(player, GOLD_SPARKLE, false,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.22, 0.22, 0.22, 0.01);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35f, 1.4f);
    }
}
