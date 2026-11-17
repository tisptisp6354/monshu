package com.farmrpg.jobs;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Server-owned, per-player selections. A slot is never overwritten once selected. */
public final class JobData {
    private JobData() {}
    public static final String NONE = "none";
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FarmRPGJobs.MODID);
    private static final Supplier<AttachmentType<String>> COMBAT = ATTACHMENTS.register("combat_job",
            () -> AttachmentType.builder(() -> NONE).serialize(Codec.STRING).copyOnDeath().build());
    private static final Supplier<AttachmentType<String>> LIFE = ATTACHMENTS.register("life_job",
            () -> AttachmentType.builder(() -> NONE).serialize(Codec.STRING).copyOnDeath().build());
    private static final Map<String, String> NAMES = Map.of(
            "warrior", "전사", "archer", "궁수", "mage", "마법사", "paladin", "성기사",
            "ranger", "레인저", "assassin", "암살자", "cleric", "성직자",
            "herbalist", "약초꾼", "jeweler", "세공사", "miner", "광부");
    private static final Set<String> COMBAT_IDS = Set.of("warrior", "archer", "mage", "paladin", "ranger", "assassin", "cleric");
    private static final Set<String> LIFE_IDS = Set.of("herbalist", "jeweler", "miner");

    public static String idFromName(String name) {
        return NAMES.entrySet().stream().filter(e -> e.getValue().equals(name))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }
    public static String displayName(String id) { return NAMES.getOrDefault(id, "미선택"); }
    public static String combat(ServerPlayer player) { return player.getData(COMBAT); }
    public static String life(ServerPlayer player) { return player.getData(LIFE); }

    /** Checks the server-side life-job slot; combat jobs do not affect this result. */
    public static boolean isMiner(ServerPlayer player) {
        return "miner".equals(life(player));
    }

    public enum Result { SELECTED, ALREADY_SELECTED, INVALID }
    public static Result select(ServerPlayer player, String id) {
        Supplier<AttachmentType<String>> slot;
        if (COMBAT_IDS.contains(id)) slot = COMBAT;
        else if (LIFE_IDS.contains(id)) slot = LIFE;
        else return Result.INVALID;
        if (!NONE.equals(player.getData(slot))) return Result.ALREADY_SELECTED;
        player.setData(slot, id);
        return Result.SELECTED;
    }
}
