package com.imjustdoom.doomsmarkers;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

// For some reason Forge decided to make their own wrap on the Gui class, so I can't use a mixin for this like Fabric...
@Mod.EventBusSubscriber(modid = DoomsMarkers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientListener {
    @SubscribeEvent
    public static void onDrawOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.AIR_LEVEL.type()) {
            return;
        }

        DoomsMarkersClient.renderMarkers(event.getGuiGraphics());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
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
}
