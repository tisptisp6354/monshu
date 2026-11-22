
package com.farmrpg.jobs;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value = FarmRPGJobs.MODID, dist = Dist.CLIENT)
public class FarmRPGJobsClient {

    private static final KeyMapping OPEN_JOB_SCREEN = new KeyMapping(
            "key.farmrpgjobs.open_job_screen",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.farmrpgjobs"
    );

    public FarmRPGJobsClient(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {
        modEventBus.addListener(
                FarmRPGJobsClient::registerKeys
        );

        NeoForge.EVENT_BUS.addListener(
                FarmRPGJobsClient::onClientTick
        );
    }

    private static void registerKeys(
            RegisterKeyMappingsEvent event
    ) {
        event.register(OPEN_JOB_SCREEN);
    }

    private static void onClientTick(
            ClientTickEvent.Post event
    ) {
        while (OPEN_JOB_SCREEN.consumeClick()) {

            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player != null
                    && minecraft.screen == null) {

                minecraft.setScreen(
                        new JobSelectionScreen()
                );
            }
        }
    }
}
