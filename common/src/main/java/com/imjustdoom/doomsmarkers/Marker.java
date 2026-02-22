package com.imjustdoom.doomsmarkers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Marker {
    public static Codec<Marker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(Marker::getUuid),
            Vec3.CODEC.fieldOf("position").forGetter(Marker::getPosition),
            Codec.FLOAT.listOf().fieldOf("colour").forGetter(Marker::getColour),
            Codec.INT.fieldOf("iconIndex").forGetter(Marker::getIconIndex),
            ItemStack.CODEC.fieldOf("itemIcon").forGetter(Marker::getItemIcon),
            Level.RESOURCE_KEY_CODEC.optionalFieldOf("dimension", Level.OVERWORLD).forGetter(Marker::getDimension)
    ).apply(instance, Marker::new));

    private UUID uuid;
    private Vec3 position;
    private List<Float> colour;
    private int iconIndex;
    private ItemStack itemIcon;
    private ResourceKey<Level> dimension;

    public Marker()  {
        this(null, List.of(1f, 1f, 1f, 1f), 0);
    }

    public Marker(Vec3 position, List<Float> colour, int iconIndex) {
        this(UUID.randomUUID(), position, colour, iconIndex, ItemStack.EMPTY, Level.OVERWORLD);
    }

    public Marker(Vec3 position, List<Float> colour, int iconIndex, ResourceKey<Level> dimension) {
        this(UUID.randomUUID(), position, colour, iconIndex, ItemStack.EMPTY, dimension);
    }

    public Marker(Vec3 position, List<Float> colour, Item itemIcon, ResourceKey<Level> dimension) {
        this(position, colour, -1, itemIcon, dimension);
    }

    public Marker(Vec3 position, List<Float> colour, int iconIndex, Item itemIcon, ResourceKey<Level> dimension) {
        this(UUID.randomUUID(), position, colour, iconIndex, new ItemStack(itemIcon), dimension);
    }

    public Marker(UUID uuid, Vec3 position, List<Float> colour, int iconIndex, ItemStack itemIcon, ResourceKey<Level> dimension) {
        this.uuid = uuid;
        this.position = position;
        this.colour = colour;
        this.iconIndex = iconIndex;
        this.itemIcon = itemIcon;
        this.dimension = dimension;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public List<Float> getColour() {
        return this.colour;
    }

    public void setColour(float[] colour) {
        List<Float> list = new ArrayList<>();
        for (float value : colour) {
            list.add(value);
        }
        this.colour = list;
    }

    public int getIconIndex() {
        return this.iconIndex;
    }

    public void setIconIndex(int iconIndex) {
        this.iconIndex = iconIndex;
    }

    public void changeIconIndex(boolean negative) {
        if (negative) {
            if (this.iconIndex-- <= -1) {
                this.iconIndex = DoomsMarkers.MARKER_ICONS.size() - 1;
            }
        } else {
            if (this.iconIndex + 1 >= DoomsMarkers.MARKER_ICONS.size()) {
                this.iconIndex = -1;
            } else {
                this.iconIndex++;
            }
        }
    }

    public ItemStack getItemIcon() {
        return this.itemIcon;
    }

    public void setItemIcon(Item itemIcon) {
        this.itemIcon = new ItemStack(itemIcon);
    }

    public ResourceKey<Level> getDimension() {
        return this.dimension;
    }

    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    @Override
    public String toString() {
        return getPosition().toString() + ", " + getColour() + ", " + getIconIndex() + ", " + getDimension();
    }
}
