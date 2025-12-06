package com.imjustdoom.doomsmarkers;

import com.imjustdoom.doomsmarkers.payload.ClientboundAddMarkerPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DoomsMarkers {
    public static final String MOD_ID = "doomsmarkers";
    public static final String MOD_NAME = "Doom's Markers";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static final List<ResourceLocation> MARKER_ICONS = new ArrayList<>();

    public static final int MAX_MARKERS_PER_PLAYER = 50;

    public static void init() {
        MARKER_ICONS.add(ResourceLocation.fromNamespaceAndPath("doomsmarkers", "textures/block_marker.png"));
        MARKER_ICONS.add(ResourceLocation.fromNamespaceAndPath("doomsmarkers", "textures/diamond_marker.png"));
        MARKER_ICONS.add(ResourceLocation.fromNamespaceAndPath("doomsmarkers", "textures/monster_marker.png"));
        MARKER_ICONS.add(ResourceLocation.fromNamespaceAndPath("doomsmarkers", "textures/square_marker.png"));
        MARKER_ICONS.add(ResourceLocation.fromNamespaceAndPath("doomsmarkers", "textures/grave_marker.png"));
        MARKER_ICONS.add(ResourceLocation.fromNamespaceAndPath("doomsmarkers", "textures/skele_marker.png"));
    }

    public static float[] argbIntToFloatArray(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return new float[]{r / 255f, g / 255f, b / 255f, a / 255f};
    }

    public static void sendMarkerToPlayer(ServerPlayer player, Marker marker) {
        player.connection.send(new ClientboundCustomPayloadPacket(new ClientboundAddMarkerPayload(marker)));
    }

    public static Vec3 getWorldPosFromDecoration(MapItemSavedData mapData, MapDecoration decoration) {
        byte x = decoration.x();
        byte z = decoration.y();

        if (decoration.type() == MapDecorationTypes.PLAYER_OFF_MAP || decoration.type() == MapDecorationTypes.PLAYER_OFF_LIMITS) {
            throw new IllegalArgumentException("Cannot reverse clamped decoration: position is out of map bounds");
        }

        int scale = mapData.scale;
        int i = 1 << scale;

        double centerX = mapData.centerX;
        double centerZ = mapData.centerZ;

        double levelX = centerX + x / 2.0 * i;
        double levelZ = centerZ + z / 2.0 * i;

        return new Vec3(levelX, 70, levelZ);
    }

}