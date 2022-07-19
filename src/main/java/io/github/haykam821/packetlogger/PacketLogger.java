package io.github.haykam821.packetlogger;

import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.haykam821.packetlogger.mixin.CustomPayloadC2SPacketAccessor;
import io.github.haykam821.packetlogger.mixin.CustomPayloadS2CPacketAccessor;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Field;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.util.Identifier;

import net.fabricmc.api.ModInitializer;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import java.util.Arrays;

public class PacketLogger implements ModInitializer {
	public static final String MOD_ID = "Packet Logger";
	private static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static ModConfig CONFIG = null;
    private static MemoryMappingTree mappings;
    private static int nsI; // intermediary
    private static int nsN; // named

    @Override
	public void onInitialize() {
		CONFIG = AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new).getConfig();
        if ( CONFIG.useMappings ) {
            initMappings();
        }
	}

	private static Identifier getChannel(Packet<?> packet) {
		if (packet instanceof CustomPayloadC2SPacketAccessor) {
			return ((CustomPayloadC2SPacketAccessor) packet).getChannel();
		} else if (packet instanceof CustomPayloadS2CPacketAccessor) {
			return ((CustomPayloadS2CPacketAccessor) packet).getChannel();
		}
		return null;
	}

	private static String getSideName(NetworkSide side) {
		if (side == NetworkSide.CLIENTBOUND) return "client";
		if (side == NetworkSide.SERVERBOUND) return "server";

		return side.name().toLowerCase(Locale.ROOT);
	}

	public static void logSentPacket(Packet<?> packet, NetworkSide side) {
        if ( CONFIG == null || !CONFIG.logSent ) return;
		String sideName = PacketLogger.getSideName(side);
                String data = "";

                Field[] allFields = packet.getClass().getDeclaredFields();
                for (Field field : allFields) {
                    field.setAccessible(true);
                    try {
                        data += "f: " + field.getName() + "; d: " + field.get(packet) + " / ";
                    }catch(IllegalAccessException ex) {
                        data += "f: " + field.getName() + "; exception";
                    }
                }
                
		Identifier channel = PacketLogger.getChannel(packet);
		if (channel != null) {
			LOGGER.info("Sending packet with channel '{}' ({}) / data {}", channel, sideName, data);
			return;
		}

		LOGGER.info("SEND '{}' ({}) / data {}", unmap(packet.getClass().getName()), sideName, data);
	}

	public static void logReceivedPacket(Packet<?> packet, NetworkSide side) {
        if ( CONFIG == null || !CONFIG.logReceived ) return;
		String sideName = PacketLogger.getSideName(side);
		String data = "";
		
                Field[] allFields = packet.getClass().getDeclaredFields();
                for (Field field : allFields) {
                    field.setAccessible(true);
                    try {
                        data += "f: " + field.getName() + "; d: " + field.get(packet) + " / ";
                    }catch(IllegalAccessException ex) {
                        data += "f: " + field.getName() + "; exception";
                    }
                }

		Identifier channel = PacketLogger.getChannel(packet);
		if (channel != null) {
			LOGGER.info("Received packet with channel '{}' ({}) / data {}", channel, sideName, data);
			return;
		}

		LOGGER.info("RECV '{}' ({}) / data {}", unmap(packet.getClass().getName()), sideName, data);
	}

	public static void logReceivedPacket(ByteBuf buffer) {
        if ( CONFIG == null || !CONFIG.logReceived || !CONFIG.logHex ) return;
		StringBuilder data = new StringBuilder();
		buffer.markReaderIndex();
		char[] hex = "0123456789ABCDEF".toCharArray();
		
		while(buffer.readableBytes() > 0) {
			byte d = buffer.readByte();
			data.append(hex[(d >> 4) & 0x0F]);
			data.append(hex[(d) & 0x0F]);
			data.append(" ");
		}
		buffer.resetReaderIndex();
		
		LOGGER.info("Received packet HEX: {}", data.toString());
	}

    private static void initMappings() {
        String path = CONFIG.mapPath.replaceFirst("^~", System.getProperty("user.home"));
        mappings = readMappings(Paths.get(path));
        if ( mappings != null ) {
            LOGGER.info("got " + mappings.getClasses().size() + " classes from " + path + " with namespaces " + mappings.getDstNamespaces());
            nsI = mappings.getDstNamespaces().indexOf("intermediary");
            nsN = mappings.getDstNamespaces().indexOf("named");

            mappings.setIndexByDstNames(true);
        }
    }

    private static MemoryMappingTree readMappings(Path input) {
        try (BufferedReader reader = Files.newBufferedReader(input)) {
            MemoryMappingTree mappingTree = new MemoryMappingTree();
            MappingReader.read(reader, mappingTree);

            return mappingTree;
        } catch (IOException e) {
            LOGGER.error("Failed to read mappings: ", e);
        }
        return null;
    }

    private static String unmap(String src) {
        if ( !CONFIG.useMappings )
            return src;
        if ( mappings == null )
            return src;
        ClassMapping c = mappings.getClass(src.replace(".","/"), nsI);
        if ( c == null )
            return src;
        String dst = c.getDstName(nsN);
        if ( dst == null )
            return src;
        return dst;
    }
}
