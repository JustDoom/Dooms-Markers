package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.network.PacketHandler;
import com.imjustdoom.doomsmarkers.network.PacketRegistry;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleCustomPayload", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundCustomPayloadPacket;getData()Lnet/minecraft/network/FriendlyByteBuf;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    public void handleMyPackets(ClientboundCustomPayloadPacket packet, CallbackInfo ci, ResourceLocation location, FriendlyByteBuf friendlyByteBuf) {
        if (!location.getNamespace().equals("doomsmarkers")) {
            return;
        }

        PacketHandler handler = PacketRegistry.get(location);
        if (handler != null) {
            handler.handle(null, packet.getData());
            ci.cancel();
        }
    }
}