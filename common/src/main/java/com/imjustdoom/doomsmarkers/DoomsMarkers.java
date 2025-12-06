package com.imjustdoom.doomsmarkers;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoomsMarkers {
    public static final String MOD_ID = "doomsmarkers";
    public static final String MOD_NAME = "Doom's Markers";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static final ResourceLocation MARKER_SYNC_PACKET = new ResourceLocation("doomsmarkers", "marker");
    public static final ResourceLocation ADD_MARKER_PACKET = new ResourceLocation("doomsmarkers", "add");
    public static final ResourceLocation CALCULATE_MAP_MARKER_PACKET = new ResourceLocation("doomsmarkers", "calculate_map");
    public static final ResourceLocation DELETE_MARKER_PACKET = new ResourceLocation("doomsmarkers", "delete");
    public static final ResourceLocation UPDATE_MARKER_PACKET = new ResourceLocation("doomsmarkers", "update");

    public static final List<ResourceLocation> MARKER_ICONS = new ArrayList<>();

    public static final int MAX_MARKERS_PER_PLAYER = 50;

    public static void init() {
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/block_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/diamond_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/monster_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/square_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/grave_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/skele_marker.png"));
    }

    public static float[] argbIntToFloatArray(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return new float[]{r / 255f, g / 255f, b / 255f, a / 255f};
    }

    public static void sendMarkerToPlayer(ServerPlayer player, Marker marker) {
        try {
            Tag encodedMarker = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker).getOrThrow(false, null);

            CompoundTag wrapper = new CompoundTag();
            wrapper.put("data", encodedMarker);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeNbt(wrapper);

            player.connection.send(new ClientboundCustomPayloadPacket(DoomsMarkers.ADD_MARKER_PACKET, buf));
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to encode the Markers: {}", e.getMessage());
        }
    }

    public static Vec3 getWorldPosFromDecoration(MapItemSavedData mapData, MapDecoration decoration) {
        byte x = decoration.getX();
        byte z = decoration.getY();

        if (decoration.getType() == MapDecoration.Type.PLAYER_OFF_MAP || decoration.getType() == MapDecoration.Type.PLAYER_OFF_LIMITS) {
            throw new IllegalArgumentException("Cannot reverse clamped decoration: position is out of map bounds");
        }

        int scale = mapData.scale;
        int i = 1 << scale;

        double centerX = mapData.centerX;
        double centerZ = mapData.centerZ;

        double f = x / 2.0;
        double f1 = z / 2.0;

        double levelX = centerX + f * i;
        double levelZ = centerZ + f1 * i;

        return new Vec3(levelX, 70, levelZ);
    }

}