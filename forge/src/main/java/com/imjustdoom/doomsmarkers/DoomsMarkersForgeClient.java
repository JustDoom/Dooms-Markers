package com.imjustdoom.doomsmarkers;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = DoomsMarkers.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class DoomsMarkersForgeClient {
    private DoomsMarkersForgeClient() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DoomsMarkersClient.MARKER_KEY_MAPPING);
        event.register(DoomsMarkersClient.TOGGLE_MARKER_KEY_MAPPING);
        event.register(DoomsMarkersClient.DELETE_MARKER_MAPPING);
    }
}