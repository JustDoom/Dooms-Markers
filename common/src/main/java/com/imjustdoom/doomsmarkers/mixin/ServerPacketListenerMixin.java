package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
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
        switch (location.getPath()) {
            case "add" -> {
                CompoundTag wrapper = packet.getData().readNbt();
                if (wrapper != null && wrapper.contains("data", Tag.TAG_COMPOUND)) {
                    CompoundTag compoundTag = wrapper.getCompound("data");
                    Marker loaded = Marker.CODEC.parse(NbtOps.INSTANCE, compoundTag)
                            .getOrThrow(false, err -> System.err.println("Failed to parse markers: " + err));
                    DoomsMarkers.MARKERS.get(getPlayer()).add(loaded);
                }
            }
            case "calculate_map" -> {
                ItemStack itemStack = getPlayer().getItemInHand(getPlayer().getUsedItemHand());
                if (itemStack.getItem() != Items.FILLED_MAP) {
                    // Tell player there was an issue
                    break;
                }

                MapItemSavedData data = MapItem.getSavedData(itemStack, getPlayer().level());
                if (data == null) {
                    System.out.println("No data to fetch");
                    return;
                }

                for (MapDecoration decoration : data.getDecorations()) {
                    if (decoration.getType() == MapDecoration.Type.PLAYER_OFF_MAP || decoration.getType() == MapDecoration.Type.PLAYER_OFF_LIMITS) {
                        continue;
                    }

                    Marker marker = new Marker(DoomsMarkers.getWorldPosFromDecoration(data, decoration), List.of(1f, 1f, 1f, 1f), 2);
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