
package com.farmrpg.jobs;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

@Mod(FarmRPGJobs.MODID)
public class FarmRPGJobs {

    public static final String MODID = "farmrpgjobs";

    public static final Logger LOGGER = LogUtils.getLogger();

    public FarmRPGJobs(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, AlchemyPotionSettings.SPEC);
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.TYPES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        modBus.addListener(ModItems::addCreativeTabItems);
        JobData.ATTACHMENTS.register(modBus);
        modBus.addListener(JobNetworking::register);
        NeoForge.EVENT_BUS.addListener(JobCommands::register);
        NeoForge.EVENT_BUS.addListener(MinerPassive::onDrops);
        NeoForge.EVENT_BUS.addListener(MinerPassive::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(MinerPassive::onLogin);
        NeoForge.EVENT_BUS.addListener(MinerPassive::onRespawn);
        LOGGER.info("Farm RPG Jobs mod loaded!");
    }
}
