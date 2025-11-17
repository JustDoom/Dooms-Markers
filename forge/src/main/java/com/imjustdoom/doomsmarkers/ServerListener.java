package com.imjustdoom.doomsmarkers;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class ServerListener {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        DoomsMarkers.MARKERS.putIfAbsent(player, new ArrayList<>());
        Tag encodedList = Marker.CODEC.listOf().encodeStart(NbtOps.INSTANCE, DoomsMarkers.MARKERS.get(player))
                .getOrThrow(false, err -> System.err.println("Failed to encode markers: " + err));

        CompoundTag wrapper = new CompoundTag();
        wrapper.put("data", encodedList);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeNbt(wrapper);

        player.connection.send(new ClientboundCustomPayloadPacket(DoomsMarkers.MARKER_SYNC_PACKET, buf));
    }

    @SubscribeEvent
    public static void onServerShutdown(ServerStoppingEvent event) {
        DoomsMarkers.MARKERS.clear();
    }
}
