package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkersClient;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.network.packet.AddMarkerPacket;
import com.imjustdoom.doomsmarkers.network.packet.CalculateMapMarkerPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Minecraft.class)
public abstract class ClientMinecraftMixin {
    @Unique
    private boolean doomsMarkers$markerDownLast = false;

    @Inject(at = @At("RETURN"), method = "tick")
    private void init(CallbackInfo info) {
        Minecraft minecraft = (Minecraft) (Object) this;

        if (minecraft.player == null) {
            return;
        }

        if (DoomsMarkersClient.TOGGLE_MARKER_KEY_MAPPING.consumeClick()) {
            DoomsMarkersClient.TOGGLED_MARKERS = !DoomsMarkersClient.TOGGLED_MARKERS;
            minecraft.player.sendSystemMessage(Component.literal("Toggled Markers ").withStyle(ChatFormatting.RESET)
                    .append(
                            Component.literal(DoomsMarkersClient.TOGGLED_MARKERS ? "ON" : "OFF")
                                    .withStyle(DoomsMarkersClient.TOGGLED_MARKERS ? ChatFormatting.GREEN : ChatFormatting.RED)
                    )
            );
        }

        boolean currentDown = DoomsMarkersClient.MARKER_KEY_MAPPING.isDown();

        if (!this.doomsMarkers$markerDownLast && currentDown) {
            DoomsMarkersClient.KEY_USED_THIS_HOLD = false;
        }

        if (this.doomsMarkers$markerDownLast && !currentDown && !DoomsMarkersClient.KEY_USED_THIS_HOLD && DoomsMarkersClient.TOGGLED_MARKERS) {
            ItemStack itemStack = minecraft.player.getItemInHand(minecraft.player.getUsedItemHand());
            if (itemStack.getItem() == Items.FILLED_MAP) {
                minecraft.player.connection.send(new ServerboundCustomPayloadPacket(CalculateMapMarkerPacket.CALCULATE_MAP_MARKER_PACKET, new FriendlyByteBuf(Unpooled.buffer())));
            } else {
                Vec3 pos = minecraft.player.position();
                Marker marker = new Marker("player_made", new Vec3(pos.x, pos.y + 0.75f, pos.z), List.of(1f, 1f, 1f, 1f), 1, ItemStack.EMPTY.getItem(), minecraft.player.level().dimension(), true, true, -1);
                DoomsMarkersClient.sendEncodedMarker(minecraft, marker, AddMarkerPacket.ADD_MARKER_PACKET);
            }
        }

        this.doomsMarkers$markerDownLast = currentDown;
    }
}