package com.imjustdoom.doomsmarkers.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface PacketHandler {
    ResourceLocation getId();
    void handle(Player player, FriendlyByteBuf data);
}
