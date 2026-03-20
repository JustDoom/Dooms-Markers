package com.imjustdoom.doomsmarkers;

import com.imjustdoom.doomsmarkers.command.Commands;
import com.imjustdoom.doomsmarkers.command.argument.MarkerArgument;
import com.imjustdoom.doomsmarkers.command.argument.MarkerArgumentInfo;
import com.imjustdoom.doomsmarkers.command.argument.MarkerLiteArgument;
import com.imjustdoom.doomsmarkers.command.argument.MarkerLiteArgumentInfo;
import com.imjustdoom.doomsmarkers.network.packet.SyncMarkerPacket;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

public class DoomsMarkersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            KeyBindingHelper.registerKeyBinding(DoomsMarkersClient.MARKER_KEY_MAPPING);
            KeyBindingHelper.registerKeyBinding(DoomsMarkersClient.TOGGLE_MARKER_KEY_MAPPING);
        }

        // Handle player joining and sending the markers to the client
        ServerPlayConnectionEvents.INIT.register((listener, server) -> {
            try {
                Tag encodedList = Marker.CODEC.listOf().encodeStart(NbtOps.INSTANCE, ((ServerPlayerInterface) listener.getPlayer()).getMarkers()).getOrThrow(false, null);

                CompoundTag wrapper = new CompoundTag();
                wrapper.put("data", encodedList);

                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeNbt(wrapper);

                listener.send(new ClientboundCustomPayloadPacket(SyncMarkerPacket.SYNC_MARKER_PACKET, buf));
            } catch (Exception e) {
                DoomsMarkers.LOG.error("Unable to encode the Markers: ", e);
            }
        });

        ArgumentTypeRegistry.registerArgumentType(new ResourceLocation(DoomsMarkers.MOD_ID, "marker"), MarkerArgument.class, new MarkerArgumentInfo());
        ArgumentTypeRegistry.registerArgumentType(new ResourceLocation(DoomsMarkers.MOD_ID, "marker_lite"), MarkerLiteArgument.class, new MarkerLiteArgumentInfo());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Commands.register(dispatcher, registryAccess));

        DoomsMarkers.init();
    }
}
