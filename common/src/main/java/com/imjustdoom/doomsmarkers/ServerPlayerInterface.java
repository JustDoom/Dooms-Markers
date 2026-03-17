package com.imjustdoom.doomsmarkers;

import java.util.List;

/**
 * A custom ServerPlayer interface to allow getting the Marker list from player classes. Don't implement anywhere.
 * Only use by casting a ServerPlayer to this while still keeping the ServerPlayer instance if needed.
 */
public interface ServerPlayerInterface {
    List<Marker> getMarkers();
}
