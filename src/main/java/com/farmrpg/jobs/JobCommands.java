package com.farmrpg.jobs;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class JobCommands {
    private JobCommands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("job").then(Commands.literal("info").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            player.sendSystemMessage(Component.literal("[직업] 전투 직업: " + JobData.displayName(JobData.combat(player))));
            player.sendSystemMessage(Component.literal("[직업] 생활 직업: " + JobData.displayName(JobData.life(player))));
            return 1;
        })));
    }
}
