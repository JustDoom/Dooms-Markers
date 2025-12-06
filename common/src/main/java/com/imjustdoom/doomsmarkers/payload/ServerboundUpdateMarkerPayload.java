package com.imjustdoom.doomsmarkers.payload;

import com.imjustdoom.doomsmarkers.Marker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundUpdateMarkerPayload(Marker marker) implements CustomPacketPayload {
    public static final ResourceLocation PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath("doomsmarkers", "update");
    public static final Type<ServerboundUpdateMarkerPayload> ID = new Type<>(PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundUpdateMarkerPayload> CODEC = StreamCodec.composite(
            Marker.STREAM_CODEC,
            ServerboundUpdateMarkerPayload::marker,
            ServerboundUpdateMarkerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
