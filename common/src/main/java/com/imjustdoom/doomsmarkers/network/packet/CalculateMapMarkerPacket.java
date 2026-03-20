package com.imjustdoom.doomsmarkers.network.packet;

import com.imjustdoom.doomsmarkers.ColourUtil;
import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.ServerPlayerInterface;
import com.imjustdoom.doomsmarkers.network.PacketHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CalculateMapMarkerPacket implements PacketHandler {
    public static final ResourceLocation CALCULATE_MAP_MARKER_PACKET = new ResourceLocation("doomsmarkers", "calculate_map");

    @Override
    public ResourceLocation getId() {
        return CALCULATE_MAP_MARKER_PACKET;
    }

    @Override
    public void handle(Player player, FriendlyByteBuf data) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayerInterface markerPlayer = (ServerPlayerInterface) serverPlayer;

            // Get item in hand and make sure it is a valid item
            ItemStack itemStack = serverPlayer.getItemInHand(serverPlayer.getUsedItemHand());
            if (itemStack.getItem() != Items.FILLED_MAP) {
                serverPlayer.sendSystemMessage(Component.literal("Unable to detect map item").withStyle(ChatFormatting.RED));
                return;
            }

            // Make sure the map has data to be read
            MapItemSavedData mapData = MapItem.getSavedData(itemStack, serverPlayer.level());
            if (mapData == null) {
                DoomsMarkers.LOG.info("No data to fetch");
                return;
            }

            for (MapBanner banner : mapData.getBanners()) {
                if (markerPlayer.getMarkers().size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
                    serverPlayer.sendSystemMessage(Component.literal("You are at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :(").withStyle(ChatFormatting.RED));
                    return;
                }

                List<Float> colour = ColourUtil.argbIntToFloatList(banner.getColor().getTextColor());
                Marker marker = new Marker("map", new Vec3(banner.getPos().getX(), banner.getPos().getY() + 0.75f, banner.getPos().getZ()), colour, 2, ItemStack.EMPTY.getItem(), serverPlayer.serverLevel().dimension(), true, true, -1);
                markerPlayer.getMarkers().add(marker);

                DoomsMarkers.sendMarkerToPlayer(serverPlayer, marker);
            }

            for (MapDecoration decoration : mapData.getDecorations()) {
                if (markerPlayer.getMarkers().size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
                    serverPlayer.sendSystemMessage(Component.literal("You are at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :(").withStyle(ChatFormatting.RED));
                    return;
                }

                if (decoration.getType() != MapDecoration.Type.RED_X
                        && decoration.getType() != MapDecoration.Type.MONUMENT
                        && decoration.getType() != MapDecoration.Type.MANSION
                        && decoration.getType() != MapDecoration.Type.TARGET_POINT
                        && decoration.getType() != MapDecoration.Type.TARGET_X
                        && decoration.getType() != MapDecoration.Type.BLUE_MARKER
                        && decoration.getType() != MapDecoration.Type.RED_MARKER
                        && decoration.getType() != MapDecoration.Type.PLAYER) {
                    continue;
                }

                List<Float> colour;
                if (decoration.getType().hasMapColor()) {
                    colour = ColourUtil.argbIntToFloatList(decoration.getType().getMapColor());
                } else {
                    colour = List.of(1f, 1f, 1f, 1f);
                }
                Marker marker = new Marker("map", DoomsMarkers.getWorldPosFromDecoration(mapData, decoration), colour, 2, ItemStack.EMPTY.getItem(), serverPlayer.serverLevel().dimension(), true, true, -1);
                markerPlayer.getMarkers().add(marker);

                DoomsMarkers.sendMarkerToPlayer(serverPlayer, marker);
            }
        }
    }
}
