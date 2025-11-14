package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(value = ServerGamePacketListenerImpl.class)
public abstract class ServerPacketListenerMixin {
    @Shadow
    public abstract ServerPlayer getPlayer();

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    public void handleMyPackets(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        ResourceLocation location = packet.getIdentifier();
        if (!location.getNamespace().equals("doomsmarkers")) {
            return;
        }

        if (!DoomsMarkers.MARKERS.containsKey(getPlayer())) {
            DoomsMarkers.MARKERS.put(getPlayer(), new ArrayList<>());
        }

        SWITCH:
        switch (location.getPath()) {
            case "add" -> {
                if (DoomsMarkers.MARKERS.get(getPlayer()).size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
                    getPlayer().sendSystemMessage(Component.literal("You are at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :(").withStyle(ChatFormatting.RED));
                    break;
                }
                CompoundTag wrapper = packet.getData().readNbt();
                if (wrapper != null && wrapper.contains("data", Tag.TAG_COMPOUND)) {
                    CompoundTag compoundTag = wrapper.getCompound("data");
                    Marker loaded = Marker.CODEC.parse(NbtOps.INSTANCE, compoundTag)
                            .getOrThrow(false, err -> System.err.println("Failed to parse markers: " + err));
                    DoomsMarkers.MARKERS.get(getPlayer()).add(loaded);
                    DoomsMarkers.sendMarkerToPlayer(getPlayer(), loaded);
                }
            }
            case "calculate_map" -> {
                ItemStack itemStack = getPlayer().getItemInHand(getPlayer().getUsedItemHand());
                if (itemStack.getItem() != Items.FILLED_MAP) {
                    getPlayer().sendSystemMessage(Component.literal("Unable to detect map item").withStyle(ChatFormatting.RED));
                    break;
                }

                MapItemSavedData data = MapItem.getSavedData(itemStack, getPlayer().level());
                if (data == null) {
                    System.out.println("No data to fetch");
                    return;
                }

                for (MapBanner banner : data.getBanners()) {
                    if (DoomsMarkers.MARKERS.get(getPlayer()).size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
                        getPlayer().sendSystemMessage(Component.literal("You are at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :(").withStyle(ChatFormatting.RED));
                        break SWITCH;
                    }

                    List<Float> colour = new ArrayList<>();
                    for (float value : DoomsMarkers.argbIntToFloatArray(banner.getColor().getTextColor())) {
                        colour.add(value);
                    }

                    Marker marker = new Marker(new Vec3(banner.getPos().getX(), banner.getPos().getY() + 0.75f, banner.getPos().getZ()), colour, 2);
                    DoomsMarkers.MARKERS.get(getPlayer()).add(marker);

                    DoomsMarkers.sendMarkerToPlayer(getPlayer(), marker);
                }

                for (MapDecoration decoration : data.getDecorations()) {
                    if (DoomsMarkers.MARKERS.get(getPlayer()).size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
                        getPlayer().sendSystemMessage(Component.literal("You are at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :(").withStyle(ChatFormatting.RED));
                        break SWITCH;
                    }

                    if (decoration.getType() != MapDecoration.Type.RED_X
                            && decoration.getType() != MapDecoration.Type.MONUMENT
                            && decoration.getType() != MapDecoration.Type.MANSION
                            && decoration.getType() != MapDecoration.Type.TARGET_POINT
                            && decoration.getType() != MapDecoration.Type.TARGET_X
                            && decoration.getType() != MapDecoration.Type.BLUE_MARKER
                            && decoration.getType() != MapDecoration.Type.RED_MARKER
                            && decoration.getType() != MapDecoration.Type.PLAYER) {
                        continue;
                    }

                    List<Float> colour;
                    if (decoration.getType().hasMapColor()) {
                        colour = new ArrayList<>();
                        for (float value : DoomsMarkers.argbIntToFloatArray(decoration.getType().getMapColor())) {
                            colour.add(value);
                        }
                    } else {
                        colour = List.of(1f, 1f, 1f, 1f);
                    }
                    Marker marker = new Marker(DoomsMarkers.getWorldPosFromDecoration(data, decoration), colour, 2);
                    DoomsMarkers.MARKERS.get(getPlayer()).add(marker);

                    DoomsMarkers.sendMarkerToPlayer(getPlayer(), marker);
                }
            }
            case "delete" -> {
                CompoundTag wrapper = packet.getData().readNbt();
                if (wrapper != null && wrapper.contains("uuid", Tag.TAG_STRING)) {
                    UUID uuid = UUID.fromString(wrapper.getString("uuid"));
                    for (Marker marker : DoomsMarkers.MARKERS.get(getPlayer())) {
                        if (marker.getUuid().equals(uuid)) {
                            DoomsMarkers.MARKERS.get(getPlayer()).remove(marker);
                            break;
                        }
                    }
                }
            }
            case "update" -> {
                CompoundTag wrapper = packet.getData().readNbt();
                if (wrapper != null && wrapper.contains("data", Tag.TAG_COMPOUND)) {
                    CompoundTag compoundTag = wrapper.getCompound("data");
                    Marker loaded = Marker.CODEC.parse(NbtOps.INSTANCE, compoundTag)
                            .getOrThrow(false, err -> System.err.println("Failed to parse markers: " + err));
                    for (Marker marker : DoomsMarkers.MARKERS.get(getPlayer())) {
                        if (marker.getUuid().equals(loaded.getUuid())) {
                            DoomsMarkers.MARKERS.get(getPlayer()).remove(marker);
                            DoomsMarkers.MARKERS.get(getPlayer()).add(loaded);
                            break;
                        }
                    }
                }
            }
        }

        ci.cancel();
    }
}