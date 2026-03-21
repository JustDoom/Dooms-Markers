package com.imjustdoom.doomsmarkers.network.packet;

import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.ServerPlayerInterface;
import com.imjustdoom.doomsmarkers.network.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class UpdateMarkerPacket implements PacketHandler {
    public static final ResourceLocation UPDATE_MARKER_PACKET = new ResourceLocation("doomsmarkers", "update");

    @Override
    public ResourceLocation getId() {
        return UPDATE_MARKER_PACKET;
    }

    @Override
    public void handle(Player player, FriendlyByteBuf data) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayerInterface markerPlayer = (ServerPlayerInterface) serverPlayer;
            Marker loaded = Marker.getMarkerFromBuffer(data);
            if (loaded == null) {
                return;
            }

            Marker markerToRemove = null;
            for (Marker marker : markerPlayer.getMarkers()) {
                if (marker.getUuid().equals(loaded.getUuid()) && marker.canPlayerCustomise()) {
                    markerToRemove = marker;
                    break;
                }
            }

            if (markerToRemove != null) {
                markerPlayer.getMarkers().remove(markerToRemove);
                markerPlayer.getMarkers().add(loaded);
            }
        }
    }
}
