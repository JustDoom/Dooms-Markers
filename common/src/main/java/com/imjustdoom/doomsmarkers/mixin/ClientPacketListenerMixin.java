package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkersClient;
import com.imjustdoom.doomsmarkers.Marker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(value = ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleCustomPayload", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundCustomPayloadPacket;getData()Lnet/minecraft/network/FriendlyByteBuf;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    public void handleMyPackets(ClientboundCustomPayloadPacket packet, CallbackInfo ci, ResourceLocation location, FriendlyByteBuf friendlyByteBuf) {
        if (!location.getNamespace().equals("doomsmarkers")) {
            return;
        }

        if (location.getPath().equals("marker")) {
            CompoundTag wrapper = packet.getData().readNbt();
            if (wrapper != null && wrapper.contains("data", Tag.TAG_LIST)) {
                ListTag dataList = wrapper.getList("data", Tag.TAG_COMPOUND);
                List<Marker> loaded = Marker.CODEC.listOf().parse(NbtOps.INSTANCE, dataList)
                        .getOrThrow(false, err -> System.err.println("Failed to parse markers: " + err));
                DoomsMarkersClient.MARKERS.clear();
                DoomsMarkersClient.MARKERS.addAll(loaded);
            }
        } else if (location.getPath().equals("add")) {
            CompoundTag wrapper = packet.getData().readNbt();
            if (wrapper != null && wrapper.contains("data", Tag.TAG_COMPOUND)) {
                CompoundTag compoundTag = wrapper.getCompound("data");
                Marker loaded = Marker.CODEC.parse(NbtOps.INSTANCE, compoundTag)
                        .getOrThrow(false, err -> System.err.println("Failed to parse markers: " + err));
                DoomsMarkersClient.MARKERS.add(loaded);
            }
        }

        ci.cancel();
    }
}