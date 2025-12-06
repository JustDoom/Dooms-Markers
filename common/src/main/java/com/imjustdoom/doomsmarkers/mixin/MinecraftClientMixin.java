package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkersClient;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.payload.ServerboundCalculateMapPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
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
public abstract class MinecraftClientMixin {
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
        }

        boolean currentDown = DoomsMarkersClient.MARKER_KEY_MAPPING.isDown();

        if (!this.doomsMarkers$markerDownLast && currentDown) {
            DoomsMarkersClient.KEY_USED_THIS_HOLD = false;
        }

        if (this.doomsMarkers$markerDownLast && !currentDown && !DoomsMarkersClient.KEY_USED_THIS_HOLD && DoomsMarkersClient.TOGGLED_MARKERS) {
            ItemStack itemStack = minecraft.player.getItemInHand(minecraft.player.getUsedItemHand());
            if (itemStack.getItem() == Items.FILLED_MAP) {
                minecraft.player.connection.send(new ServerboundCustomPayloadPacket(new ServerboundCalculateMapPayload()));
            } else {
                Vec3 pos = minecraft.player.position();
                Marker marker = new Marker(new Vec3(pos.x, pos.y + 0.75f, pos.z), List.of(1f, 1f, 1f, 1f), 1);
                DoomsMarkersClient.sendMarkerToServer(marker);
            }
        }

        this.doomsMarkers$markerDownLast = currentDown;
    }
}