package com.imjustdoom.doomsmarkers.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundCalculateMapPayload() implements CustomPacketPayload {
    public static final ResourceLocation PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath("doomsmarkers", "calculate_map");
    public static final Type<ServerboundCalculateMapPayload> ID = new Type<>(PAYLOAD_ID);
    public static final ServerboundCalculateMapPayload INSTANCE = new ServerboundCalculateMapPayload();
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCalculateMapPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
