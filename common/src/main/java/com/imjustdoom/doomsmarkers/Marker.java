package com.imjustdoom.doomsmarkers;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Marker {
    public static final Codec<ItemStack> CUSTOM_ITEMSTACK_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<Pair<ItemStack, T>> result = ItemStack.OPTIONAL_CODEC.decode(ops, input);
            return DataResult.success(result.resultOrPartial(errorMsg -> {
            }).orElse(Pair.of(ItemStack.EMPTY, input)));
        }

        @Override
        public <T> DataResult<T> encode(ItemStack stack, DynamicOps<T> ops, T prefix) {
            return ItemStack.OPTIONAL_CODEC.encode(stack, ops, prefix);
        }
    };

    public static Codec<Marker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(Marker::getUuid),
            Vec3.CODEC.fieldOf("position").forGetter(Marker::getPosition),
            Codec.FLOAT.listOf().fieldOf("colour").forGetter(Marker::getColour),
            Codec.INT.fieldOf("iconIndex").forGetter(Marker::getIconIndex),
            CUSTOM_ITEMSTACK_CODEC.optionalFieldOf("itemIcon", ItemStack.EMPTY).forGetter(Marker::getItemIcon)
    ).apply(instance, Marker::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Marker> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Marker::getUuid,
            ByteBufCodecs.fromCodec(Vec3.CODEC), Marker::getPosition,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.FLOAT), Marker::getColour,
            ByteBufCodecs.VAR_INT, Marker::getIconIndex,
            ItemStack.OPTIONAL_STREAM_CODEC, Marker::getItemIcon,
            Marker::new
    );

    private final UUID uuid;
    private final Vec3 position;
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

    public Marker(UUID uuid, Vec3 position, List<Float> colour, int iconIndex, ItemStack itemIcon) {
        this.uuid = uuid;
        this.position = position;
        this.colour = colour;
        this.iconIndex = iconIndex;
        this.itemIcon = itemIcon.isEmpty() || itemIcon.is(Items.AIR) ? ItemStack.EMPTY : itemIcon;
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
        return getPosition().toString() + ", " + getColour() + ", " + getIconIndex() + ", " + getItemIcon() + ", " + getUuid();
    }
}
