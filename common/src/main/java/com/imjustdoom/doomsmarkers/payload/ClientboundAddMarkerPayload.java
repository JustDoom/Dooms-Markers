package com.imjustdoom.doomsmarkers.payload;

import com.imjustdoom.doomsmarkers.Marker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundAddMarkerPayload(Marker marker) implements CustomPacketPayload {
    public static final ResourceLocation PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath("doomsmarkers", "cadd");
    public static final CustomPacketPayload.Type<ClientboundAddMarkerPayload> ID = new CustomPacketPayload.Type<>(PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundAddMarkerPayload> CODEC = StreamCodec.composite(
            Marker.STREAM_CODEC,
            ClientboundAddMarkerPayload::marker,
            ClientboundAddMarkerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
