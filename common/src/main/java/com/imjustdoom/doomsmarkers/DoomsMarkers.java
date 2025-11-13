package com.imjustdoom.doomsmarkers;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class DoomsMarkers {
    public static final KeyMapping MARKER_KEY_MAPPING = new KeyMapping("category.doomsmarkers.use", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.doomsmarkers");

    public static final List<ResourceLocation> MARKER_ICONS = new ArrayList<>();

    public static final List<Marker> FOCUSED_MARKERS = new ArrayList<>();
    public static final List<Marker> MARKERS = new ArrayList<>();

    public static void init() {
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/block_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/diamond_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/monster_marker.png"));
        MARKER_ICONS.add(new ResourceLocation("doomsmarkers", "textures/square_marker.png"));

        MARKERS.add(new Marker(new BlockPos(0, 70, 0), new float[]{1, 0.75f, 0.2f, 1}, 0));
        MARKERS.add(new Marker(new BlockPos(100, 70, 25), new float[]{0.9f, 0.35f, 0.72f, 1}, 1));
        MARKERS.add(new Marker(new BlockPos(-50, 30, -20), new float[]{0.24f, 0.5f, 0.298f, 1}, 2));

        System.out.println(MARKERS);
    }
}