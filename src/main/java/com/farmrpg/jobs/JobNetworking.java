package com.farmrpg.jobs;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

public final class JobNetworking {
    private JobNetworking() {}
    public record SelectJob(String jobId) implements CustomPacketPayload {
        public static final Type<SelectJob> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FarmRPGJobs.MODID, "select_job"));
        public static final StreamCodec<ByteBuf, SelectJob> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SelectJob::jobId, SelectJob::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(SelectJob.TYPE, SelectJob.CODEC, JobNetworking::handle);
        event.registrar("1").playToClient(LifeSync.TYPE, LifeSync.CODEC, (payload, context) ->
                JobData.syncClientLife(context.player(), payload.jobId()));
    }
    public record LifeSync(String jobId) implements CustomPacketPayload {
        public static final Type<LifeSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FarmRPGJobs.MODID, "life_sync"));
        public static final StreamCodec<ByteBuf, LifeSync> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, LifeSync::jobId, LifeSync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void syncLife(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new LifeSync(JobData.life(player)));
    }
    private static void handle(SelectJob payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        JobData.Result result = JobData.select(player, payload.jobId());
        if (result == JobData.Result.SELECTED) syncLife(player);
        String message = switch (result) {
            case SELECTED -> "[직업] " + JobData.displayName(payload.jobId()) + " 직업을 선택했습니다!";
            case ALREADY_SELECTED -> "[직업] 이 분류의 직업을 이미 선택하여 변경할 수 없습니다.";
            case INVALID -> "[직업] 유효하지 않은 직업입니다.";
        };
        player.sendSystemMessage(Component.literal(message));
    }
}
