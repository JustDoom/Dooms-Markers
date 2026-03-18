package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.network.PacketHandler;
import com.imjustdoom.doomsmarkers.network.PacketRegistry;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class)
public abstract class ServerPacketListenerMixin {
    @Shadow
    public abstract ServerPlayer getPlayer();

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    public void handleMyPackets(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        ResourceLocation location = packet.getIdentifier();
        if (!location.getNamespace().equals("doomsmarkers")) {
            return;
        }

        PacketHandler handler = PacketRegistry.get(location);
        if (handler != null) {
            handler.handle(getPlayer(), packet.getData());
            ci.cancel();
        }
    }
}