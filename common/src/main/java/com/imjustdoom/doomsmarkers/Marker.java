package com.imjustdoom.doomsmarkers;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record Marker(BlockPos position, float[] colour, ResourceLocation icon) {

}
