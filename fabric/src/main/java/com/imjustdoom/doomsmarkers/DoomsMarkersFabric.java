package com.imjustdoom.doomsmarkers;

import com.imjustdoom.doomsmarkers.payload.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;

import java.util.List;

public class DoomsMarkersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            KeyBindingHelper.registerKeyBinding(DoomsMarkersClient.MARKER_KEY_MAPPING);
            KeyBindingHelper.registerKeyBinding(DoomsMarkersClient.TOGGLE_MARKER_KEY_MAPPING);
        }

        PayloadTypeRegistry.playS2C().register(ClientboundAddMarkerPayload.ID, ClientboundAddMarkerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundMarkerSyncPayload.ID, ClientboundMarkerSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundUpdateMarkerPayload.ID, ClientboundUpdateMarkerPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(ServerboundAddMarkerPayload.ID, ServerboundAddMarkerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ServerboundCalculateMapPayload.ID, ServerboundCalculateMapPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ServerboundDeleteMarkerPayload.ID, ServerboundDeleteMarkerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ServerboundUpdateMarkerPayload.ID, ServerboundUpdateMarkerPayload.CODEC);

        ServerPlayConnectionEvents.INIT.register((listener, server) -> {
            List<Marker> markers = ((ServerPlayerInterface) listener.getPlayer()).getMarkers();
            ClientboundMarkerSyncPayload sync = new ClientboundMarkerSyncPayload(markers);
            listener.send(new ClientboundCustomPayloadPacket(sync));
        });

        DoomsMarkers.init();
    }
}
