package io.github.haykam821.packetlogger.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.haykam821.packetlogger.PacketLogger;
import net.minecraft.network.DecoderHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

@Mixin(DecoderHandler.class)
public class PacketDecoderMixin {

    @Inject(method = "decode", at = @At("HEAD"))
    private void logReceivedPacket(ChannelHandlerContext var0, ByteBuf var1, List<Object> var2, CallbackInfo ci) {
        PacketLogger.logReceivedPacket(var1);
    }
}
