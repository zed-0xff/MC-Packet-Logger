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

    public List<String> ignores = Arrays.asList(
            "KeepAliveC2SPacket",
            "KeepAliveS2CPacket",
            "PlayPingS2CPacket",
            "PlayPongC2SPacket"
            );
}
