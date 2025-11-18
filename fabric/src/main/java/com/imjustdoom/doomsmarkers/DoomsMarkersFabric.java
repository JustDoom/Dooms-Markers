package com.imjustdoom.doomsmarkers;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;

public class DoomsMarkersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            KeyBindingHelper.registerKeyBinding(DoomsMarkersClient.MARKER_KEY_MAPPING);
            KeyBindingHelper.registerKeyBinding(DoomsMarkersClient.TOGGLE_MARKER_KEY_MAPPING);
        }

        ServerPlayConnectionEvents.INIT.register((listener, server) -> {
            Tag encodedList = Marker.CODEC.listOf().encodeStart(NbtOps.INSTANCE, ((ServerPlayerInterface) listener.getPlayer()).getMarkers())
                    .getOrThrow(false, err -> System.err.println("Failed to encode markers: " + err));

            CompoundTag wrapper = new CompoundTag();
            wrapper.put("data", encodedList);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeNbt(wrapper);

            listener.send(new ClientboundCustomPayloadPacket(DoomsMarkers.MARKER_SYNC_PACKET, buf));
        });

        DoomsMarkers.init();
    }
}
