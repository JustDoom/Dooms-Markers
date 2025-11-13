package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin {
    @Inject(method = "swapPaint", at = @At(value = "HEAD"), cancellable = true)
    public void onHudScroll(double direction, CallbackInfo ci) {
        if (DoomsMarkers.MARKER_KEY_MAPPING.isDown()) {
            if (DoomsMarkers.FOCUSED_MARKERS.isEmpty()) {
                return;
            }

            int index = (int) Math.signum(direction);
            Marker marker = DoomsMarkers.FOCUSED_MARKERS.get(0);
            if (index != 0) {
                marker.changeIconIndex(index == -1);
                DoomsMarkers.KEY_USED_THIS_HOLD = true;
                ci.cancel();
            }
        }
    }
}
