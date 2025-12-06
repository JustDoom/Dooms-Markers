package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.ServerPlayerInterface;
import com.imjustdoom.doomsmarkers.payload.ServerboundAddMarkerPayload;
import com.imjustdoom.doomsmarkers.payload.ServerboundCalculateMapPayload;
import com.imjustdoom.doomsmarkers.payload.ServerboundDeleteMarkerPayload;
import com.imjustdoom.doomsmarkers.payload.ServerboundUpdateMarkerPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ServerGamePacketListenerImpl.class)
public abstract class ServerPacketListenerMixin {
    @Shadow
    public abstract ServerPlayer getPlayer();

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    public void handleMyPackets(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        ServerPlayerInterface serverPlayer = (ServerPlayerInterface) getPlayer();

        SWITCH:
        switch (packet.payload()) {
            case ServerboundAddMarkerPayload addMarkerPayload -> {
                if (serverPlayer.getMarkers().size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
                    getPlayer().sendSystemMessage(Component.literal("You are at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :(").withStyle(ChatFormatting.RED));
                    break;
                }

                serverPlayer.getMarkers().add(addMarkerPayload.marker());
                DoomsMarkers.sendMarkerToPlayer(getPlayer(), addMarkerPayload.marker());
            }
            case ServerboundCalculateMapPayload ignored -> {
                ItemStack itemStack = getPlayer().getItemInHand(getPlayer().getUsedItemHand());
                if (itemStack.getItem() != Items.FILLED_MAP) {
                    getPlayer().sendSystemMessage(Component.literal("Unable to detect map item").withStyle(ChatFormatting.RED));
                    break;
                }

                MapItemSavedData data = MapItem.getSavedData(itemStack, getPlayer().level());
                if (data == null) {
                    DoomsMarkers.LOG.info("No data to fetch");
                    return;
                }

                for (MapBanner banner : data.getBanners()) {
                    if (serverPlayer.getMarkers().size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
                        getPlayer().sendSystemMessage(Component.literal("You are at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :(").withStyle(ChatFormatting.RED));
                        break SWITCH;
                    }

                    List<Float> colour = new ArrayList<>();
                    for (float value : DoomsMarkers.argbIntToFloatArray(banner.color().getTextColor())) {
                        colour.add(value);
                    }

                    Marker marker = new Marker(new Vec3(banner.pos().getX(), banner.pos().getY() + 0.75f, banner.pos().getZ()), colour, 2);
                    serverPlayer.getMarkers().add(marker);

                    DoomsMarkers.sendMarkerToPlayer(getPlayer(), marker);
                }

                for (MapDecoration decoration : data.getDecorations()) {
                    if (serverPlayer.getMarkers().size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
                        getPlayer().sendSystemMessage(Component.literal("You are at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :(").withStyle(ChatFormatting.RED));
                        break SWITCH;
                    }

                    if (decoration.type() != MapDecorationTypes.RED_X
                            && decoration.type() != MapDecorationTypes.OCEAN_MONUMENT
                            && decoration.type() != MapDecorationTypes.WOODLAND_MANSION
                            && decoration.type() != MapDecorationTypes.TARGET_POINT
                            && decoration.type() != MapDecorationTypes.TARGET_X
                            && decoration.type() != MapDecorationTypes.BLUE_MARKER
                            && decoration.type() != MapDecorationTypes.RED_MARKER
                            && decoration.type() != MapDecorationTypes.PLAYER) {
                        continue;
                    }

                    List<Float> colour;
                    if (decoration.type().value().hasMapColor()) {
                        colour = new ArrayList<>();
                        for (float value : DoomsMarkers.argbIntToFloatArray(decoration.type().value().mapColor())) {
                            colour.add(value);
                        }
                    } else {
                        colour = List.of(1f, 1f, 1f, 1f);
                    }
                    Marker marker = new Marker(DoomsMarkers.getWorldPosFromDecoration(data, decoration), colour, 2);
                    serverPlayer.getMarkers().add(marker);

                    DoomsMarkers.sendMarkerToPlayer(getPlayer(), marker);
                }
            }
            case ServerboundDeleteMarkerPayload deleteMarkerPayload -> {
                for (Marker marker : serverPlayer.getMarkers()) {
                    if (marker.getUuid().equals(deleteMarkerPayload.uuid())) {
                        serverPlayer.getMarkers().remove(marker);
                        break;
                    }
                }
            }
            case ServerboundUpdateMarkerPayload updateMarkerPayload -> {
                for (Marker marker : serverPlayer.getMarkers()) {
                    if (marker.getUuid().equals(updateMarkerPayload.marker().getUuid())) {
                        serverPlayer.getMarkers().remove(marker);
                        serverPlayer.getMarkers().add(updateMarkerPayload.marker());
                        break;
                    }
                }
            }
            default -> {}
        }

        ci.cancel();
    }
}