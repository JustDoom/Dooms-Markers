package com.imjustdoom.doomsmarkers;

import com.imjustdoom.doomsmarkers.command.argument.MarkerArgument;
import com.imjustdoom.doomsmarkers.command.argument.MarkerArgumentInfo;
import com.imjustdoom.doomsmarkers.command.argument.MarkerLiteArgument;
import com.imjustdoom.doomsmarkers.command.argument.MarkerLiteArgumentInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(DoomsMarkers.MOD_ID)
public class DoomsMarkersForge {
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, DoomsMarkers.MOD_ID);
    public static final RegistryObject<ArgumentTypeInfo<?, ?>> MARKER_ARGUMENT = COMMAND_ARGUMENT_TYPES.register("marker", () -> ArgumentTypeInfos.registerByClass(MarkerArgument.class, new MarkerArgumentInfo()));
    public static final RegistryObject<ArgumentTypeInfo<?, ?>> MARKER_LITE_ARGUMENT = COMMAND_ARGUMENT_TYPES.register("marker_lite", () -> ArgumentTypeInfos.registerByClass(MarkerLiteArgument.class, new MarkerLiteArgumentInfo()));

    public DoomsMarkersForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(ServerListener.class);

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(DoomsMarkersClient.MARKER_KEY_MAPPING);
            event.register(DoomsMarkersClient.TOGGLE_MARKER_KEY_MAPPING);
        });

        COMMAND_ARGUMENT_TYPES.register(modBus);

        DoomsMarkers.init();
    }
}