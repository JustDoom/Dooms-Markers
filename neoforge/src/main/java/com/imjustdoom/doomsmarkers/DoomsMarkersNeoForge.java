package com.imjustdoom.doomsmarkers;


import com.imjustdoom.doomsmarkers.payload.ClientboundMarkerSyncPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(DoomsMarkers.MOD_ID)
public class DoomsMarkersNeoForge {

    public DoomsMarkersNeoForge(IEventBus eventBus) {
//        eventBus.register(ServerListener.class);

        eventBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(DoomsMarkersClient.MARKER_KEY_MAPPING);
            event.register(DoomsMarkersClient.TOGGLE_MARKER_KEY_MAPPING);
        });

        eventBus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            player.connection.send(new ClientboundCustomPayloadPacket(new ClientboundMarkerSyncPayload(((ServerPlayerInterface) player).getMarkers())));
        });

        DoomsMarkers.init();
    }
}