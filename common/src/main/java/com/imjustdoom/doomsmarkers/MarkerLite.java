package com.imjustdoom.doomsmarkers;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class MarkerLite {
    private final UUID uuid;
    private final String type;
    private final Vec3 position;
    private final ResourceKey<Level> dimension;

    public MarkerLite(UUID uuid, String type, Vec3 position, ResourceKey<Level> dimension) {
        this.uuid = uuid;
        this.type = type;
        this.position = position;
        this.dimension = dimension;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getType() {
        return this.type;
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public ResourceKey<Level> getDimension() {
        return this.dimension;
    }

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
