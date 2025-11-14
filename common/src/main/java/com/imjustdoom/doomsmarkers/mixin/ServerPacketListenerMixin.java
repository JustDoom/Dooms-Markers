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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
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