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
}
