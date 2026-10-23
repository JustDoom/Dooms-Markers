package com.imjustdoom.doomsmarkers;

import com.imjustdoom.doomsmarkers.network.PacketRegistry;
import com.imjustdoom.doomsmarkers.network.packet.*;
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
}