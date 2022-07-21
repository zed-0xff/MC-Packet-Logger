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

    boolean logHex  = false;
    boolean useMappings = false;
    String  mapPath = "~/.gradle/caches/fabric-loom/1.19/net.fabricmc.yarn.1_19.1.19+build.4-v2/mappings.tiny";

    List<String> stripPrefixes = Arrays.asList(
        "net/minecraft/network/packet/s2c/play/",
        "net/minecraft/network/packet/c2s/play/"
        );

    List<String> ignores = Arrays.asList(
            "BlockBreakingProgressS2CPacket",
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
}
