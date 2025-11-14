package com.imjustdoom.doomsmarkers;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DoomsMarkers.MOD_ID)
public class DoomsMarkersForge {
    public DoomsMarkersForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(DoomsMarkersClient.MARKER_KEY_MAPPING);
            event.register(DoomsMarkersClient.TOGGLE_MARKER_KEY_MAPPING);
        });

        DoomsMarkers.init();
    }
}