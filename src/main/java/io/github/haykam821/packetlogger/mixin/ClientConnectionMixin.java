package io.github.haykam821.packetlogger.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.haykam821.packetlogger.PacketLogger;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.NetworkState;
import net.minecraft.network.PacketCallbacks;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Shadow
    @Final
    private NetworkSide side;

    @Inject(method = "sendInternal(Lnet/minecraft/network/Packet;Lnet/minecraft/network/PacketCallbacks;Lnet/minecraft/network/NetworkState;Lnet/minecraft/network/NetworkState;)V", at = @At("HEAD"))
    private void logSentPacket(Packet<?> packet, PacketCallbacks callbacks, NetworkState packetState, NetworkState currentState, CallbackInfo ci) {
        PacketLogger.logSentPacket(packet, this.side);
    }

    @Inject(method = "handlePacket(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;)V", at = @At("HEAD"))
    private static void logReceivedPacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        PacketLogger.logReceivedPacket(packet, ((ClientConnectionAccessor) listener.getConnection()).getSide());
    }
}
