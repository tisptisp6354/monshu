
package com.farmrpg.jobs;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

@Mod(FarmRPGJobs.MODID)
public class FarmRPGJobs {

    public static final String MODID = "farmrpgjobs";

    public static final Logger LOGGER = LogUtils.getLogger();

    public FarmRPGJobs(IEventBus modBus) {
        JobData.ATTACHMENTS.register(modBus);
        modBus.addListener(JobNetworking::register);
        NeoForge.EVENT_BUS.addListener(JobCommands::register);
        LOGGER.info("Farm RPG Jobs mod loaded!");
    }
}
