package com.imjustdoom.doomsmarkers;

import net.minecraft.resources.ResourceLocation;

public record MarkerIcon(String name, ResourceLocation resourceLocation) {
    public MarkerIcon(String name) {
        this(name, new ResourceLocation(DoomsMarkers.MOD_ID, "textures/markers/" + name + ".png"));
    }
}
