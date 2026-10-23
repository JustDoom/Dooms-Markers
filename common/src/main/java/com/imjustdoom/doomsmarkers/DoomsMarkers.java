package com.imjustdoom.doomsmarkers;

import com.imjustdoom.doomsmarkers.network.PacketRegistry;
import com.imjustdoom.doomsmarkers.network.packet.*;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

/**
 * Handles the shared and server only code
 */
public class DoomsMarkers {
    public static final String MOD_ID = "doomsmarkers";
    public static final String MOD_NAME = "Doom's Markers";
    public static final Logger LOG = (Logger) LogManager.getLogger(MOD_NAME);

    // Add icons to the icon list
    public static void init() {
        Config.get();

        switch (Config.get().logging.toLowerCase()) {
            case "warn":
                LOG.setLevel(Level.WARN);
                break;
            case "error":
                LOG.setLevel(Level.ERROR);
                break;
            case "debug":
                LOG.setLevel(Level.DEBUG);
                break;
            default:
                break;
        }

        PacketRegistry.register(new AddMarkerPacket());
        PacketRegistry.register(new CalculateMapMarkerPacket());
        PacketRegistry.register(new DeleteMarkerPacket());
        PacketRegistry.register(new SyncMarkerPacket());
        PacketRegistry.register(new UpdateMarkerPacket());
    }

    public static void sendMarkerToPlayer(ServerPlayer player, Marker marker) {
        try {
            Tag encodedMarker = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker).getOrThrow(false, null);

            CompoundTag wrapper = new CompoundTag();
            wrapper.put("data", encodedMarker);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeNbt(wrapper);

            player.connection.send(new ClientboundCustomPayloadPacket(AddMarkerPacket.ADD_MARKER_PACKET, buf));
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to encode the Markers: ", e);
        }
    }

    public static void removeMarkerFromPlayer(ServerPlayer player, Marker marker) {
        try {
            Tag encodedMarker = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker).getOrThrow(false, null);

            CompoundTag wrapper = new CompoundTag();
            wrapper.put("data", encodedMarker);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeNbt(wrapper);

            player.connection.send(new ClientboundCustomPayloadPacket(DeleteMarkerPacket.DELETE_MARKER_PACKET, buf));
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to encode the Markers: ", e);
        }
    }

    public static Vec3 getWorldPosFromDecoration(MapItemSavedData mapData, MapDecoration decoration) {
        byte x = decoration.getX();
        byte z = decoration.getY();

        if (decoration.getType() == MapDecoration.Type.PLAYER_OFF_MAP || decoration.getType() == MapDecoration.Type.PLAYER_OFF_LIMITS) {
            throw new IllegalArgumentException("Cannot reverse clamped decoration: position is out of map bounds");
        }

        int scale = mapData.scale;
        int i = 1 << scale;

        double centerX = mapData.centerX;
        double centerZ = mapData.centerZ;

        double f = x / 2.0;
        double f1 = z / 2.0;

        double levelX = centerX + f * i;
        double levelZ = centerZ + f1 * i;

        return new Vec3(levelX, 70, levelZ);
    }
}