package com.imjustdoom.doomsmarkers.network.packet;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.DoomsMarkersClient;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.network.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class SyncMarkerPacket implements PacketHandler {
    public static final ResourceLocation SYNC_MARKER_PACKET = new ResourceLocation("doomsmarkers", "sync");

    @Override
    public ResourceLocation getId() {
        return SYNC_MARKER_PACKET;
    }

    @Override
    public void handle(Player player, FriendlyByteBuf data) {
        CompoundTag wrapper = data.readNbt();
        if (wrapper != null && wrapper.contains("data", Tag.TAG_LIST)) {
            try {
                ListTag dataList = wrapper.getList("data", Tag.TAG_COMPOUND);
                List<Marker> loaded = Marker.CODEC.listOf().parse(NbtOps.INSTANCE, dataList).getOrThrow(false, null);
                DoomsMarkersClient.MARKERS.clear();
                DoomsMarkersClient.MARKERS.addAll(loaded);
            } catch (Exception e) {
                DoomsMarkers.LOG.error("Unable to encode the Markers: {}", e.getMessage());
            }
        }
    }
}
