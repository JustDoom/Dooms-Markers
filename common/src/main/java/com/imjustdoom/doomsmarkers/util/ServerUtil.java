package com.imjustdoom.doomsmarkers.util;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.network.packet.AddMarkerPacket;
import com.imjustdoom.doomsmarkers.network.packet.DeleteMarkerPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server side methods
 */
public class ServerUtil {
    public static void sendMarkerToPlayer(ServerPlayer player, Marker marker) {
        try {
            Tag encodedMarker = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker).getOrThrow(false, null);

            CompoundTag wrapper = new CompoundTag();
            wrapper.put("data", encodedMarker);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeNbt(wrapper);

            player.connection.send(new ClientboundCustomPayloadPacket(AddMarkerPacket.ADD_MARKER_PACKET, buf));
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to encode the Markers: ", e);
        }
    }

    public static void removeMarkerFromPlayer(ServerPlayer player, Marker marker) {
        try {
            Tag encodedMarker = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker).getOrThrow(false, null);

            CompoundTag wrapper = new CompoundTag();
            wrapper.put("data", encodedMarker);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeNbt(wrapper);

            player.connection.send(new ClientboundCustomPayloadPacket(DeleteMarkerPacket.DELETE_MARKER_PACKET, buf));
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to encode the Markers: ", e);
        }
    }
}
