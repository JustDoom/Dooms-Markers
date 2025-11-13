package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        boolean currentDown = DoomsMarkers.MARKER_KEY_MAPPING.isDown();

        if (!this.doomsMarkers$markerDownLast && currentDown) {
            DoomsMarkers.KEY_USED_THIS_HOLD = false;
        }

        if (this.doomsMarkers$markerDownLast && !currentDown && !DoomsMarkers.KEY_USED_THIS_HOLD) {
            Vec3 pos = minecraft.player.position();
            DoomsMarkers.MARKERS.add(new Marker(new Vec3(pos.x, pos.y + 0.75f, pos.z), new float[]{1, 1, 1, 1}, 2));
        }

        this.doomsMarkers$markerDownLast = currentDown;
    }
}