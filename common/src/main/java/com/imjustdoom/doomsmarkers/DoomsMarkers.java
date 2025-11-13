package com.imjustdoom.doomsmarkers;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class DoomsMarkers {
    public static final ResourceLocation BLOCK_MARKER = new ResourceLocation("doomsmarkers", "textures/block_marker.png");
    public static final ResourceLocation DIAMOND_MARKER = new ResourceLocation("doomsmarkers", "textures/diamond_marker.png");
    public static final ResourceLocation MONSTER_MARKER = new ResourceLocation("doomsmarkers", "textures/monster_marker.png");
    public static final ResourceLocation SQUARE_MARKER = new ResourceLocation("doomsmarkers", "textures/square_marker.png");

    public static final List<Marker> MARKERS = new ArrayList<>();

    public static void init() {
        MARKERS.add(new Marker(new BlockPos(0, 70, 0), new float[]{1, 0.75f, 0.2f, 1}, BLOCK_MARKER));
        MARKERS.add(new Marker(new BlockPos(100, 70, 25), new float[]{0.9f, 0.35f, 0.72f, 1}, SQUARE_MARKER));
        MARKERS.add(new Marker(new BlockPos(-50, 30, -20), new float[]{0.24f, 0.5f, 0.298f, 1}, DIAMOND_MARKER));
    }
}