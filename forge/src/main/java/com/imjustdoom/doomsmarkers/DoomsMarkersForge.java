package com.imjustdoom.doomsmarkers;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class DoomsMarkersForge {
    public DoomsMarkersForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(DoomsMarkers.MARKER_KEY_MAPPING);
        });

        DoomsMarkers.init();
    }
}