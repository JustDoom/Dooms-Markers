package com.imjustdoom.doomsmarkers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
    public static final ResourceLocation DELETE_MARKER_PACKET = new ResourceLocation("doomsmarkers", "delete");
    public static final ResourceLocation UPDATE_MARKER_PACKET = new ResourceLocation("doomsmarkers", "update");

    public static final List<ResourceLocation> MARKER_ICONS = new ArrayList<>();
    public static final Map<ServerPlayer, List<Marker>> MARKERS = new HashMap<>();

    public static void init() {
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/block_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/diamond_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/monster_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/square_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/grave_marker.png"));
    }

    public static float[] argbIntToFloatArray(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return new float[]{r / 255f, g / 255f, b / 255f, a / 255f};
    }
}