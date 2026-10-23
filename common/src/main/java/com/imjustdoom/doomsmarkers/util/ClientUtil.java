package com.imjustdoom.doomsmarkers.util;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.DoomsMarkersClient;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.Markers;
import com.imjustdoom.doomsmarkers.network.packet.DeleteMarkerPacket;
import com.imjustdoom.doomsmarkers.network.packet.UpdateMarkerPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeItem;

/**
 * Client side methods
 */
public class ClientUtil {
    /**
     * Does not remove the marker from any lists. Just sends a packet to tell the server it should be removed
     * @param minecraft
     * @param marker
     */
    public static void sendRemoveMarker(Minecraft minecraft, Marker marker) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.putString("uuid", marker.getUuid().toString());

        minecraft.player.connection.send(new ServerboundCustomPayloadPacket(DeleteMarkerPacket.DELETE_MARKER_PACKET,
                new FriendlyByteBuf(Unpooled.buffer()).writeNbt(wrapper)));
    }

    /**
     * Sends a request to attempt an update to the marker
     * @param minecraft
     * @param marker
     */
    public static void sendUpdateIcon(Minecraft minecraft, Marker marker) {
        marker.setIcon(Markers.CUSTOM_ITEM, true); // Set the index so scrolling resumes from the custom item icon
        marker.setItemIcon(minecraft.player.getItemInHand(minecraft.player.getUsedItemHand()).getItem());
        DoomsMarkersClient.KEY_USED_THIS_HOLD = true;

        sendEncodedMarker(minecraft, marker, UpdateMarkerPacket.UPDATE_MARKER_PACKET);
    }

    /**
     * Sends an attempt to update the colour of a marker
     * @param minecraft
     * @param marker
     * @param dye colour to set the marker to
     */
    public static void sendUpdateColour(Minecraft minecraft, Marker marker, DyeItem dye) {
        marker.setColour(ColourUtil.argbIntToFloatList(dye.getDyeColor().getTextColor()));
        DoomsMarkersClient.KEY_USED_THIS_HOLD = true;

        sendEncodedMarker(minecraft, marker, UpdateMarkerPacket.UPDATE_MARKER_PACKET);
    }

    /**
     * Sends a marker object to the server with the specific packet type. Likely UPDATE or ADD marker packets
     * @param minecraft
     * @param marker
     * @param packet
     * @return
     */
    public static boolean sendEncodedMarker(Minecraft minecraft, Marker marker, ResourceLocation packet) {
        try {
            Tag encoded = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker).getOrThrow(false, null);

            CompoundTag wrapper = new CompoundTag();
            wrapper.put("data", encoded);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeNbt(wrapper);
            minecraft.player.connection.send(new ServerboundCustomPayloadPacket(packet, buf));
            return true;
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to encode the Markers here: ", e);
            return false;
        }
    }
}
