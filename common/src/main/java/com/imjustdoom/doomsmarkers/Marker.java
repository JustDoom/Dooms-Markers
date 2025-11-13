package com.imjustdoom.doomsmarkers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
            ItemStack.CODEC.fieldOf("itemIcon").forGetter(Marker::getItemIcon)
    ).apply(instance, Marker::new));

    private UUID uuid = UUID.randomUUID();
    private Vec3 position;
    private List<Float> colour;
    private int iconIndex;
    private ItemStack itemIcon;

    public Marker(Vec3 position, List<Float> colour, int iconIndex) {
        this(UUID.randomUUID(), position, colour, iconIndex, ItemStack.EMPTY);
    }

    public Marker(Vec3 position, List<Float> colour, Item itemIcon) {
        this(position, colour, -1, itemIcon);
    }

    public Marker(Vec3 position, List<Float> colour, int iconIndex, Item itemIcon) {
        this(UUID.randomUUID(), position, colour, iconIndex, new ItemStack(itemIcon));
    }

    public Marker(Vec3 position, List<Float> colour, int iconIndex, ItemStack itemIcon) {
        this.position = position;
        this.colour = colour;
        this.iconIndex = iconIndex;
        this.itemIcon = itemIcon;
    }

    public Marker(UUID uuid, Vec3 position, List<Float> colour, int iconIndex, ItemStack itemIcon) {
        this.uuid = uuid;
        this.position = position;
        this.colour = colour;
        this.iconIndex = iconIndex;
        this.itemIcon = itemIcon;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Vec3 getPosition() {
        return this.position;
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

    @Override
    public String toString() {
        return getPosition().toString() + ", " + getColour() + ", " + getIconIndex();
    }
}
