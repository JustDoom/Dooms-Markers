package com.imjustdoom.doomsmarkers;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DoomsMarkers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandListener {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        Commands.register(event.getDispatcher(), event.getBuildContext());
    }
}
