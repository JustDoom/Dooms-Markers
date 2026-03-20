package com.imjustdoom.doomsmarkers.network.packet;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.DoomsMarkersClient;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.ServerPlayerInterface;
import com.imjustdoom.doomsmarkers.network.PacketHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class AddMarkerPacket implements PacketHandler {
    public static final ResourceLocation ADD_MARKER_PACKET = new ResourceLocation("doomsmarkers", "add");

    @Override
    public ResourceLocation getId() {
        return ADD_MARKER_PACKET;
    }

    @Override
    public void handle(Player player, FriendlyByteBuf data) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayerInterface markerPlayer = (ServerPlayerInterface) serverPlayer;
            if (markerPlayer.getMarkers().size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
                serverPlayer.sendSystemMessage(Component.literal("You are at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :(").withStyle(ChatFormatting.RED));
                return;
            }
            Marker loaded = Marker.getMarkerFromBuffer(data);
            if (loaded == null) {
                return;
            }

            loaded.setDimension(serverPlayer.serverLevel().dimension());
            markerPlayer.getMarkers().add(loaded);
            DoomsMarkers.sendMarkerToPlayer(serverPlayer, loaded);
        } else {
            CompoundTag wrapper = data.readNbt();
            if (wrapper != null && wrapper.contains("data", Tag.TAG_COMPOUND)) {
                try {
                    CompoundTag compoundTag = wrapper.getCompound("data");
                    Marker loaded = Marker.CODEC.parse(NbtOps.INSTANCE, compoundTag).getOrThrow(false, null);
                    DoomsMarkersClient.MARKERS.add(loaded);
                } catch (Exception e) {
                    DoomsMarkers.LOG.error("Unable to encode the Markers: ", e);
                }
            }
        }
    }
}
