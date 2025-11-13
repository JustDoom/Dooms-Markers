package com.imjustdoom.doomsmarkers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoomsMarkers {
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

//        MARKERS.add(new Marker(new Vec3(0, 70, 0), List.of(1f, 0.75f, 0.2f, 1f), 0));
//        MARKERS.add(new Marker(new Vec3(100, 70, 25), List.of(0.9f, 0.35f, 0.72f, 1f), 1));
//        MARKERS.add(new Marker(new Vec3(-50, 30, -20), List.of(0.24f, 0.5f, 0.298f, 1f), 2));

        System.out.println(MARKERS);
    }

    public static float[] argbIntToFloatArray(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return new float[]{r / 255f, g / 255f, b / 255f, a / 255f};
    }
}