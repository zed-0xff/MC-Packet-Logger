package io.github.haykam821.packetlogger;

import java.util.Arrays;
import java.util.List;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.ConfigEntry.*;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.*;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = PacketLogger.MOD_ID)
public class ModConfig implements ConfigData {

    boolean logSent = false;
    boolean logReceived = false;
    boolean logTick = false;
    boolean enableAtStart = false;

    boolean logHex  = false;
    boolean useMappings = false;
    String  mapPath = "~/.gradle/caches/fabric-loom/1.19.1/net.fabricmc.yarn.1_19_1.1.19.1+build.6-v2/mappings.tiny";

    // high values likely to cause recursions and burn out your CPU ;)
    // also bloated logs, especially on BlockUpdateS2CPacket with recursion >= 5
    // one EntitySpawnS2CPacket with recursion level 5 => 500kb
    @ConfigEntry.BoundedDiscrete(min=0, max=10)
    int maxRecursion = 2;

    List<String> stripPrefixes = Arrays.asList(
        "net/minecraft/network/packet/s2c/play/",
        "net/minecraft/network/packet/c2s/play/"
        );

    // feel free to unignore
    List<String> ignores = Arrays.asList(
//            "class_2827", // 1.19.1 PlayPongC2SPacket ?
            "BlockBreakingProgressS2CPacket",
            "BlockUpdateS2CPacket",
            "BossBarS2CPacket",
            "ChunkDataS2CPacket",
            "ChunkDeltaUpdateS2CPacket",
            "EntityAnimationS2CPacket",
            "EntityEquipmentUpdateS2CPacket",
            "EntityPositionS2CPacket",
            "EntityS2CPacket$MoveRelative",
            "EntityS2CPacket$Rotate",
            "EntityS2CPacket$RotateAndMoveRelative",
            "EntitySetHeadYawS2CPacket",
            "EntityTrackerUpdateS2CPacket",
            "EntityVelocityUpdateS2CPacket",
            "GameMessageS2CPacket",
            "HandSwingC2SPacket",
            "HealthUpdateS2CPacket",
            "KeepAliveC2SPacket",
            "KeepAliveS2CPacket",
            "ParticleS2CPacket",
            "PlayPingS2CPacket",
            "PlayPongC2SPacket",
            "PlaySoundIdS2CPacket",
            "PlayerListHeaderS2CPacket",
            "PlayerListS2CPacket",
            "PlayerMoveC2SPacket$Full",
            "PlayerMoveC2SPacket$LookAndOnGround",
            "PlayerMoveC2SPacket$PositionAndOnGround",
            "ScoreboardObjectiveUpdateS2CPacket",
            "ScoreboardPlayerUpdateS2CPacket",
            "TeamS2CPacket",
            "WorldTimeUpdateS2CPacket"
            );

    // they're just too big, again feel free to unskip
    List<String> skipClasses = Arrays.asList(
            "net/minecraft/client/resource/language/TranslationStorage",
            "net/minecraft/sound/BlockSoundGroup",
            "net/minecraft/state/StateManager",
            "net/minecraft/util/registry/DefaultedRegistry",
            "net/minecraft/util/registry/RegistryEntry",
            "net/minecraft/util/registry/RegistryEntry$Reference"
            );
}
