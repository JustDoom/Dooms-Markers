package com.imjustdoom.doomsmarkers.network;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class PacketRegistry {
    private static final Map<ResourceLocation, PacketHandler> HANDLERS = new HashMap<>();

    public static void register(PacketHandler handler) {
        HANDLERS.put(handler.getId(), handler);
    }

    public static PacketHandler get(ResourceLocation id) {
        return HANDLERS.get(id);
    }
}
