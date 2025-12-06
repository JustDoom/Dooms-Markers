package com.imjustdoom.doomsmarkers;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
}
