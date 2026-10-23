package com.imjustdoom.doomsmarkers;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record MarkerLite(UUID uuid, String type, Vec3 position, ResourceKey<Level> dimension) {

    @Override
    public String toString() {
        return "Marker{" +
                "uuid=" + this.uuid +
                ", type=" + this.type +
                ", position=" + this.position +
                ", dimension=" + this.dimension +
                '}';
    }
}
