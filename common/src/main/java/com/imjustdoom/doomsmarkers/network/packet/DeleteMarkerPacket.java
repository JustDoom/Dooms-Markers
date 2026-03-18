package com.imjustdoom.doomsmarkers.network.packet;

import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.ServerPlayerInterface;
import com.imjustdoom.doomsmarkers.network.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class DeleteMarkerPacket implements PacketHandler {
    public static final ResourceLocation DELETE_MARKER_PACKET = new ResourceLocation("doomsmarkers", "delete");

    @Override
    public ResourceLocation getId() {
        return DELETE_MARKER_PACKET;
    }

    @Override
    public void handle(Player player, FriendlyByteBuf data) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayerInterface markerPlayer = (ServerPlayerInterface) serverPlayer;

            CompoundTag wrapper = data.readNbt();
            if (wrapper == null || !wrapper.contains("uuid", Tag.TAG_STRING)) {
                return;
            }

            UUID uuid = UUID.fromString(wrapper.getString("uuid"));
            for (Marker marker : markerPlayer.getMarkers()) {
                if (!marker.getUuid().equals(uuid)) {
                    continue;
                }

                if (marker.getRemoveWhenNearby() != -1) {
                    double distance = Math.sqrt(serverPlayer.distanceToSqr(marker.getPosition().x, marker.getPosition().y, marker.getPosition().z));
                    if (distance <= marker.getRemoveWhenNearby()) {
                        markerPlayer.getMarkers().remove(marker);

                        serverPlayer.sendSystemMessage(Component.literal("You have now reached a Marker!")
                                .withStyle(style -> style.withColor(marker.getColourInt())));
                        return;
                    }
                }

                if (!marker.canPlayerRemove()) {
                    continue;
                }

                markerPlayer.getMarkers().remove(marker);
                return;
            }
        }
    }
}
