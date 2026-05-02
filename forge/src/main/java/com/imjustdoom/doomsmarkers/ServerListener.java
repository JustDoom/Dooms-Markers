package com.imjustdoom.doomsmarkers;

import com.imjustdoom.doomsmarkers.payload.ClientboundMarkerSyncPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerListener {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        player.connection.send(new ClientboundCustomPayloadPacket(new ClientboundMarkerSyncPayload(((ServerPlayerInterface) player).getMarkers())));
    }

    // Forge fires PlayerEvent.Clone on respawn after death and on End -> Overworld
    // return. The Marker list lives in a @Unique mixin field on ServerPlayer, which
    // vanilla restoreFrom(...) does not copy, so without this handler all markers
    // would be silently wiped on the next autosave following a respawn.
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayerInterface oldPlayer)) return;
        if (!(event.getEntity() instanceof ServerPlayerInterface newPlayer)) return;
        newPlayer.getMarkers().clear();
        newPlayer.getMarkers().addAll(oldPlayer.getMarkers());
    }
}
