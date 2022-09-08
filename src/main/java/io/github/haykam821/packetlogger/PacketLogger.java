package io.github.haykam821.packetlogger;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.haykam821.packetlogger.mixin.CustomPayloadC2SPacketAccessor;
import io.github.haykam821.packetlogger.mixin.CustomPayloadS2CPacketAccessor;
import io.netty.buffer.ByteBuf;

import java.lang.Enum;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import net.fabricmc.api.ModInitializer;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.lwjgl.glfw.GLFW;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.common.collect.ObjectArrays;

public class PacketLogger implements ModInitializer {
    public static final String NAME = "Packet Logger";
    public static final String MOD_ID = "packet-logger";
    private static final Logger LOGGER = LogManager.getLogger(NAME);
    public static ModConfig CONFIG = null;
    private static MemoryMappingTree mappings;
    private static int nsI; // intermediary
    private static int nsN; // named
    private static HashMap<String, UnmapperProxy> unmappersCache = new HashMap<String, UnmapperProxy>();
    private static HashSet<String> skipClasses = new HashSet<String>();
    public static final KeyBinding OPEN_CONFIG = new KeyBinding("key.packet-logger.open-config", GLFW.GLFW_KEY_F10, "key.packet-logger.category");
    private static final KeyBinding TOGGLE_LOGGING = new KeyBinding("key.packet-logger.toggle-logging", GLFW.GLFW_KEY_F12, "key.packet-logger.category");
    private static boolean masterSwitch = false;
    private static int tick = 0;

    @Override
    public void onInitialize() {
        CONFIG = AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new).getConfig();
        AutoConfig.getConfigHolder(ModConfig.class).registerSaveListener((manager, data) -> {
            LOGGER.info("PacketLogger initialized");
            unmappersCache.clear();
            if ( data.useMappings ) {
                initMappings();
            }
            skipClasses = new HashSet<String>(CONFIG.skipClasses);
            return ActionResult.SUCCESS;
        });
        if ( CONFIG.useMappings ) {
            initMappings();
        }
        skipClasses = new HashSet<String>(CONFIG.skipClasses);

        KeyBindingHelper.registerKeyBinding(TOGGLE_LOGGING);
        KeyBindingHelper.registerKeyBinding(OPEN_CONFIG);
        ClientTickEvents.END_CLIENT_TICK.register(PacketLogger::clientTick);
        masterSwitch = CONFIG.enableAtStart;
    }

    public static void clientTick(MinecraftClient mc) {
        tick++;
        while (OPEN_CONFIG.wasPressed()) {
            mc.setScreen(AutoConfig.getConfigScreen(ModConfig.class, mc.currentScreen).get());
        }

        while (TOGGLE_LOGGING.wasPressed()) {
            masterSwitch = !masterSwitch;
            mc.player.sendMessage(Text.literal("Packet logging is now " + (masterSwitch ? "ON" : "OFF")));
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
        if (side == NetworkSide.CLIENTBOUND) return "C";
        if (side == NetworkSide.SERVERBOUND) return "S";

        return side.name().toLowerCase(Locale.ROOT);
    }

    private static String stripPrefixes(String s) {
        for ( String prefix : CONFIG.stripPrefixes ) {
            if (s.startsWith(prefix)) {
                return s.substring(prefix.length());
            }
        }
        return s;
    }

    // from https://github.com/dancerjohn/LibEx/blob/master/libex/src/main/java/org/libex/reflect/ReflectionUtils.java
    public static Field[] getFieldsUpTo(@NotNull Class<?> type, @Nullable Class<?> exclusiveParent) {
        Field[] result = type.getDeclaredFields();

        Class<?> parentClass = type.getSuperclass();
        if (parentClass != null && !parentClass.equals(exclusiveParent)) {
            Field[] parentClassFields = getFieldsUpTo(parentClass, exclusiveParent);
            result = ObjectArrays.concat(result, parentClassFields, Field.class);
        }

        return result;
    }

    private static UnmapperProxy getUnmapperProxy(String className) {
        UnmapperProxy um = unmappersCache.get(className);
        if ( um != null )
            return um;
        um = new UnmapperProxy(className);
        unmappersCache.put(className, um);
        return um;
    }

    private static String guessFieldName(Field field) {
        UnmapperProxy um = getUnmapperProxy( field.getDeclaringClass().getName() );
        return um.fieldName(field.getName());
    }

    private static <T> String objectToString(List<T> list, int level) {
        StringBuilder r = new StringBuilder("[");
        boolean first = true;
        for ( T value : list ) {
            if ( first ) {
                first = false;
            } else {
                r.append(", ");
            }
            r.append(objectToString(value, level + 1));
        }
        r.append("]");
        return r.toString();
    }

    private static <T> String objectToString(Optional<T> object, int level) {
        if ( level >= CONFIG.maxRecursion || object.isEmpty())
            return String.valueOf(object);

        return "Optional[" + objectToString(object.get(), level + 1) + "]";
    }

    private static String objectToString(Enum<?> object, int level) {
         return String.valueOf(object);
    }

    private static <T> String objectToString(T object, int level) {
        if ( object == null )
            return String.valueOf(object);

        if ( level >= CONFIG.maxRecursion )
            return String.valueOf(object);

        if ( object instanceof Enum ) {
            return objectToString((Enum<?>) object, level);
        }

        if ( object instanceof Optional ) {
            return objectToString((Optional<?>) object, level);
        }

        if ( object instanceof List ) {
            return objectToString((List<?>) object, level); // same level
        }

        UnmapperProxy um = getUnmapperProxy(object.getClass().getName());
        if ( !um.isMapped() )
            return object.toString();

        String className = um.className();
        if( skipClasses.contains(className) ){
            return String.format("%s{ <SKIPPED> }", stripPrefixes(className));
        }

        StringBuilder data = new StringBuilder();
        boolean first = true;
        try {
            Field[] allFields = getFieldsUpTo(object.getClass(), Object.class);
            for (Field field : allFields) {
                if ( Modifier.isStatic(field.getModifiers() )) continue;
                if ( first ) first = false; else data.append(", ");
                field.setAccessible(true);
                try {
                    data.append(guessFieldName(field)).append("=").append(objectToString(field.get(object), level + 1));
                }catch(IllegalAccessException ex) {
                    data.append(guessFieldName(field)).append("=<exception!>");
                }
            }
        } catch (java.lang.reflect.InaccessibleObjectException e) {
            LOGGER.info("[?] InaccessibleObjectException: " + object.getClass().getName() + " -> " + className);
            return String.valueOf(object);
        }

        return String.format("%s{ %s }", stripPrefixes(className), data);
    }

    private static String packetToString(Packet<?> packet) {
        UnmapperProxy um = getUnmapperProxy(packet.getClass().getName());
        if ( um.isIgnored() )
            return null;

        StringBuilder data = new StringBuilder();
        boolean first = true;

        Field[] allFields = getFieldsUpTo(packet.getClass(), Object.class);
        for (Field field : allFields) {
            if ( Modifier.isStatic(field.getModifiers() )) continue;
            if ( first ) first = false; else data.append(", ");
            field.setAccessible(true);
            try {
                if ( CONFIG.maxRecursion > 0 ) {
                    data.append(guessFieldName(field)).append("=").append(objectToString(field.get(packet), 1));
                } else {
                    data.append(guessFieldName(field)).append("=").append(field.get(packet));
                }
            }catch(IllegalAccessException ex) {
                data.append(guessFieldName(field)).append("=<exception!>");
            }
        }

        return String.format("%s{ %s }", stripPrefixes(um.className()), data.toString());
    }

    private static void logPacket(String dir, String data) {
        if ( CONFIG.logTick ) {
            LOGGER.info("[{}] {} {}", tick, dir, data);
        } else {
            LOGGER.info("{} {}", dir, data);
        }
    }

    public static void logSentPacket(Packet<?> packet, NetworkSide side) {
        if ( CONFIG == null || !masterSwitch || !CONFIG.logSent ) return;

        String data = packetToString(packet);
        if ( data == null ) return;

        String sideName = PacketLogger.getSideName(side);
        Identifier channel = PacketLogger.getChannel(packet);
        if (channel != null) {
            LOGGER.info("Sending packet with channel '{}' ({}) / data {}", channel, sideName, data);
            return;
        }

        logPacket("SEND", data);
    }

    public static void logReceivedPacket(Packet<?> packet, NetworkSide side) {
        if ( CONFIG == null || !masterSwitch || !CONFIG.logReceived ) return;

        String data = packetToString(packet);
        if ( data == null ) return;

        String sideName = PacketLogger.getSideName(side);
        Identifier channel = PacketLogger.getChannel(packet);
        if (channel != null) {
            LOGGER.info("Received packet with channel '{}' ({}) / data {}", channel, sideName, data);
            return;
        }

        logPacket("RECV", data);
    }

    public static void logReceivedPacket(ByteBuf buffer) {
        if ( CONFIG == null || !masterSwitch || !CONFIG.logReceived || !CONFIG.logHex ) return;
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

    static class UnmapperProxy {
        private String dstClassName;
        private ClassMapping classMapping = null;
        private boolean isMapped = false;

        UnmapperProxy(String srcClassName) {
            if ( !CONFIG.useMappings || mappings == null ) {
                dstClassName = srcClassName;
                return;
            }
            classMapping = mappings.getClass(srcClassName.replace(".","/"), nsI);
            if ( classMapping == null ) {
                dstClassName = srcClassName;
                return;
            }
            String dst = classMapping.getDstName(nsN);
            if ( dst == null ) {
                dstClassName = srcClassName;
                return;
            }
            isMapped = true;
            dstClassName = dst;
        }

        boolean isMapped() {
            return isMapped;
        }

        boolean isIgnored() {
            for ( String suffix : CONFIG.ignores ) {
                if ( dstClassName.endsWith(suffix) )
                    return true;
            }
            return false;
        }

        String className() {
            return dstClassName;
        }

        String fieldName(String srcFieldName) {
            if ( classMapping == null )
                return srcFieldName;
            FieldMapping fieldMapping = classMapping.getField(srcFieldName, null /* desc? */, nsI);
            if ( fieldMapping == null )
                return srcFieldName;
            String dst = fieldMapping.getDstName(nsN);
            return (dst == null) ? srcFieldName : dst;
        }
    }
}
