package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkersClient;
import com.imjustdoom.doomsmarkers.payload.ClientboundAddMarkerPayload;
import com.imjustdoom.doomsmarkers.payload.ClientboundMarkerSyncPayload;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleUnknownCustomPayload", at = @At(value = "HEAD"), cancellable = true)
    public void handleMyPackets(CustomPacketPayload packet, CallbackInfo ci) {
        switch (packet) {
            case ClientboundMarkerSyncPayload markerSyncPayload -> {
                DoomsMarkersClient.MARKERS.clear();
                DoomsMarkersClient.MARKERS.addAll(markerSyncPayload.markers());
            }
            case ClientboundAddMarkerPayload addMarkerPayload -> {
                DoomsMarkersClient.MARKERS.add(addMarkerPayload.marker());
            }
            default -> {}
        }

        ci.cancel();
    }
}