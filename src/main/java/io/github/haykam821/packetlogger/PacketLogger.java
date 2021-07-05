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

public class PacketLogger {
	private static final Logger LOGGER = LogManager.getLogger("Packet Logger");

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

		LOGGER.info("Sending packet with name '{}' ({}) / data {}", packet.getClass().getName(), sideName, data);
	}

	public static void logReceivedPacket(Packet<?> packet, NetworkSide side) {
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

		LOGGER.info("Received packet with name '{}' ({}) / data {}", packet.getClass().getName(), sideName, data);
	}

	public static void logReceivedPacket(ByteBuf buffer) {
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
}
