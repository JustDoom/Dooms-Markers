package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.DoomsMarkersClient;
import com.imjustdoom.doomsmarkers.Marker;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Inject(method = "swapPaint", at = @At(value = "HEAD"), cancellable = true)
    public void onHudScroll(double direction, CallbackInfo ci) {
        if (DoomsMarkersClient.MARKER_KEY_MAPPING.isDown() && Minecraft.getInstance().player != null) {
            if (DoomsMarkersClient.FOCUSED_MARKERS.isEmpty()) {
                return;
            }

            int index = (int) Math.signum(direction);
            Marker marker = DoomsMarkersClient.FOCUSED_MARKERS.get(0);
            if (index != 0) {
                marker.changeIconIndex(index == -1);
                DoomsMarkersClient.KEY_USED_THIS_HOLD = true;

                try {
                    Tag encoded = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker).getOrThrow(false, null);

                    CompoundTag wrapper = new CompoundTag();
                    wrapper.put("data", encoded);

                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    buf.writeNbt(wrapper);
                    Minecraft.getInstance().player.connection.send(new ServerboundCustomPayloadPacket(DoomsMarkers.UPDATE_MARKER_PACKET, buf));
                } catch (Exception e) {
                    DoomsMarkers.LOG.error("Unable to encode the Markers: {}", e.getMessage());
                }

                ci.cancel();
            }
        }
    }
}
