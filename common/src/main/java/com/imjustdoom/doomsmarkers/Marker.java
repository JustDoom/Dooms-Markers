package com.imjustdoom.doomsmarkers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Marker {
    private BlockPos position;
    private float[] colour;
    private int iconIndex;
    private ItemStack itemIcon = new ItemStack(Items.DIRT_PATH);

    public Marker(BlockPos position, float[] colour, int iconIndex) {
        this(position, colour, iconIndex, null);
    }

    public Marker(BlockPos position, float[] colour, Item itemIcon) {
        this(position, colour, -1, itemIcon);
    }

    public Marker(BlockPos position, float[] colour, int iconIndex, Item itemIcon) {
        this.position = position;
        this.colour = colour;
        this.iconIndex = iconIndex;
        if (itemIcon != null) {
            this.itemIcon = new ItemStack(itemIcon);
        }
    }

    public BlockPos getPosition() {
        return this.position;
    }

    public float[] getColour() {
        return this.colour;
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
}
