package com.imjustdoom.doomsmarkers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Marker {
    public static Codec<Marker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(Marker::getUuid),
            Codec.STRING.optionalFieldOf("type", "player_made").forGetter(Marker::getType),
            Vec3.CODEC.fieldOf("position").forGetter(Marker::getPosition),
            Codec.FLOAT.listOf().fieldOf("colour").forGetter(Marker::getColour),
            Codec.STRING.optionalFieldOf("icon").forGetter(marker -> Optional.of(marker.getIcon().name())), // Optional so it is compatible with old worlds
            Codec.INT.optionalFieldOf("iconIndex", -1).forGetter(marker -> -1), // Handle the old icon saving system
            ItemStack.CODEC.fieldOf("itemIcon").forGetter(Marker::getItemIcon),
            Level.RESOURCE_KEY_CODEC.optionalFieldOf("dimension", Level.OVERWORLD).forGetter(Marker::getDimension),
            Codec.BOOL.optionalFieldOf("canPlayerRemove", true).forGetter(Marker::canPlayerRemove),
            Codec.BOOL.optionalFieldOf("canPlayerCustomise", true).forGetter(Marker::canPlayerCustomise),
            Codec.INT.optionalFieldOf("removeWhenNearby", -1).forGetter(Marker::getRemoveWhenNearby)
    ).apply(instance, Marker::createFromCodec));

    private UUID uuid;
    private String type; // This is a manual id/type used mainly for modpack devs. It can specify the type of marker. Does not need to be unique
//    private String title; // TODO: A title that shows up when hovered. Use a nametag to rename
    private Vec3 position;
    private List<Float> colour;
    private MarkerIcon icon;
    private ItemStack itemIcon;
    private ResourceKey<Level> dimension;
    private boolean canPlayerRemove;
    private boolean canPlayerCustomise;
    private int removeWhenNearby;
    private boolean removeArmed = false;

    private int iconIndex; // Only used for scrolling through icons, not stored

    public Marker(String type)  {
        this(type, null, List.of(1f, 1f, 1f, 1f), Markers.DOT, Items.AIR, Level.OVERWORLD, true, true, -1);
    }

    public Marker(String type, Vec3 position, List<Float> colour, MarkerIcon icon, Item itemIcon, ResourceKey<Level> dimension, boolean canPlayerRemove, boolean canPlayerCustomise, int removeWhenNearby) {
        this(UUID.randomUUID(), type, position, colour, icon, new ItemStack(itemIcon), dimension, canPlayerRemove, canPlayerCustomise, removeWhenNearby);
    }

    // TODO: Maybe make a builder for this since this is getting a bit much
    public Marker(UUID uuid, String type, Vec3 position, List<Float> colour, MarkerIcon icon, ItemStack itemIcon,
                  ResourceKey<Level> dimension, boolean canPlayerRemove, boolean canPlayerCustomise, int removeWhenNearby) {
        this.uuid = uuid;
        this.type = type;
        this.position = position;
        this.colour = colour;
        this.icon = icon;
        this.itemIcon = itemIcon;
        this.dimension = dimension;
        this.canPlayerRemove = canPlayerRemove;
        this.canPlayerCustomise = canPlayerCustomise;
        this.removeWhenNearby = removeWhenNearby;

        this.iconIndex = Markers.MARKER_ICONS.indexOf(icon);
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
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

    public int getColourInt() {
        return ColourUtil.floatListToRgbInt(getColour());
    }

    public void setColour(List<Float> colour) {
        this.colour = new ArrayList<>(colour);
    }

    public MarkerIcon getIcon() {
        return this.icon;
    }

    public void setIcon(@NotNull MarkerIcon icon) {
        setIcon(icon, false);
    }

    public void setIcon(@NotNull MarkerIcon icon, boolean setIndex) {
        this.icon = icon;
        if (setIndex) this.iconIndex = Markers.MARKER_ICONS.indexOf(icon);
    }

    /**
     * Handles changing the built in icon for a marker.
     * If negative is true it will get the previous icon, otherwise the next
     * @param negative
     */
    public void changeIconIndex(boolean negative) {
        if (negative) {
            this.iconIndex--;
            if ((this.iconIndex == 0 && getItemIcon().isEmpty()) || this.iconIndex < 0) {
                this.iconIndex = Markers.MARKER_ICONS.size() - 1;
            }
        } else {
            if (this.iconIndex + 1 >= Markers.MARKER_ICONS.size())
                this.iconIndex = getItemIcon().isEmpty() ? 1 : 0;
            else this.iconIndex++;
        }

        if (this.iconIndex == 0) setIcon(Markers.CUSTOM_ITEM);
        else setIcon(Markers.MARKER_ICONS.get(this.iconIndex));
    }

    public ItemStack getItemIcon() {
        return this.itemIcon;
    }

    public void setItemIcon(@NotNull Item itemIcon) {
        this.itemIcon = new ItemStack(itemIcon);
    }

    public ResourceKey<Level> getDimension() {
        return this.dimension;
    }


    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public boolean canPlayerRemove() {
        return this.canPlayerRemove;
    }

    public void setCanPlayerRemove(boolean canPlayerRemove) {
        this.canPlayerRemove = canPlayerRemove;
    }

    public boolean canPlayerCustomise() {
        return this.canPlayerCustomise;
    }

    public void setCanPlayerCustomise(boolean canPlayerCustomise) {
        this.canPlayerCustomise = canPlayerCustomise;
    }

    public int getRemoveWhenNearby() {
        return this.removeWhenNearby;
    }

    public void setRemoveWhenNearby(int removeWhenNearby) {
        this.removeWhenNearby = removeWhenNearby;
    }

    public boolean isRemoveArmed() {
        return this.removeArmed;
    }

    public void setRemoveArmed(boolean removeArmed) {
        this.removeArmed = removeArmed;
    }

    public double distanceToSqr(Marker marker) {
        double x = getPosition().x() - marker.getPosition().x();
        double y = getPosition().y() - marker.getPosition().y();
        double z = getPosition().z() - marker.getPosition().z();
        return x * x + y * y + z * z;
    }

    public double distanceToSqr(Player player) {
        double x = getPosition().x() - player.getX();
        double y = getPosition().y() - player.getY();
        double z = getPosition().z() - player.getZ();
        return x * x + y * y + z * z;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Marker marker && getUuid().equals(marker.getUuid());
    }

    @Override
    public String toString() {
        return "Marker{" +
                "uuid=" + this.uuid +
                ", type=" + this.type +
                ", position=" + this.position +
                ", colour=" + this.colour +
                ", icon=" + this.icon +
                ", itemIcon=" + this.itemIcon +
                ", dimension=" + this.dimension +
                ", canPlayerRemove=" + this.canPlayerRemove +
                ", canPlayerCustomise=" + this.canPlayerCustomise +
                ", removeWhenNearby=" + this.removeWhenNearby +
                '}';
    }

    public static Marker getMarkerFromBuffer(FriendlyByteBuf data) {
        CompoundTag wrapper = data.readNbt();
        if (wrapper == null || !wrapper.contains("data", Tag.TAG_COMPOUND)) {
            return null;
        }

        try {
            return Marker.CODEC.parse(NbtOps.INSTANCE, wrapper.getCompound("data")).getOrThrow(false, null);
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to encode the Markers: ", e);
            return null;
        }
    }

    /**
     * Uses player instead of another marker because the location displayed is to the player and not a marker
     * This makes it less confusing for any players
     * @param player
     * @param markers
     * @return
     */
    public static boolean hasMarkerWithinDistance(Player player, List<Marker> markers) {
        for (Marker marker : markers) {
            if (Math.sqrt(marker.distanceToSqr(player)) < Config.get().minimumMarkerDistance) {
                return true;
            }
        }

        return false;
    }

    private static Marker createFromCodec(UUID uuid, String type, Vec3 position, List<Float> colour,
                                          Optional<String> icon, int iconIndex, ItemStack itemIcon,
                                          ResourceKey<Level> dimension, boolean canPlayerRemove,
                                          boolean canPlayerCustomise, int removeWhenNearby) {
        MarkerIcon actualIcon = icon.isPresent() ? Markers.getIconByName(icon.get()) : indexToIcon(iconIndex);
        return new Marker(uuid, type, position, colour, actualIcon, itemIcon, dimension, canPlayerRemove, canPlayerCustomise, removeWhenNearby);
    }

    private static MarkerIcon indexToIcon(int index) {
        if (index == -1) return Markers.CUSTOM_ITEM;
        if (index < 0 || index >= Markers.MARKER_ICONS.size()) return Markers.DOT;
        return Markers.MARKER_ICONS.get(index);
    }
}
