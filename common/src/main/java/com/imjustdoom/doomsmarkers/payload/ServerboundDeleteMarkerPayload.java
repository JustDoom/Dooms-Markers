package com.imjustdoom.doomsmarkers.payload;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ServerboundDeleteMarkerPayload(UUID uuid) implements CustomPacketPayload {
    public static final ResourceLocation PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath("doomsmarkers", "delete");
    public static final Type<ServerboundDeleteMarkerPayload> ID = new Type<>(PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDeleteMarkerPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            ServerboundDeleteMarkerPayload::uuid,
            ServerboundDeleteMarkerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
