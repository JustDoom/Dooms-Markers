package com.imjustdoom.doomsmarkers.util;

import com.imjustdoom.doomsmarkers.Config;
import com.imjustdoom.doomsmarkers.Marker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class PositionUtil {
    /**
     * Uses player instead of another marker because the location displayed is to the player and not a marker
     * This makes it less confusing for any players
     * @param player
     * @param markers
     * @return
     */
    public static boolean hasMarkerWithinDistance(Player player, List<Marker> markers) {
        for (Marker marker : markers) {
            if (Math.sqrt(marker.distanceToSqr(player)) < Config.get().minimumMarkerDistance) {
                return true;
            }
        }

        return false;
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

    public static boolean isSafeTeleportLocation(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight()) {
            return false;
        }

        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            return false;
        }

        BlockState foot = level.getBlockState(pos.below());
        return foot.isFaceSturdy(level, pos.below(), Direction.UP) && !isDangerousBlock(foot);
    }

    public static boolean isDangerousBlock(BlockState state) {
        return state.is(Blocks.LAVA)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.MAGMA_BLOCK);
    }

    // TODO: Maybe avoid specific structures?
    public static Optional<BlockPos> findSafeTeleportPosition(ServerLevel level, Vec3 center, double minRadius, double maxRadius, int maxAttempts) {
        if (minRadius < 0 || maxRadius < minRadius) {
            return Optional.empty();
        }

        RandomSource random = level.getRandom();
        BlockPos centerPos = BlockPos.containing(center);

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;

            double radius = Math.sqrt(minRadius * minRadius + random.nextDouble() * (maxRadius * maxRadius - minRadius * minRadius));
            int x = centerPos.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = centerPos.getZ() + (int) Math.round(Math.sin(angle) * radius);

            ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
            int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (surfaceY <= level.getMinBuildHeight()) {
                continue;
            }

            BlockPos feet = new BlockPos(x, surfaceY + 1, z);

            if (isSafeTeleportLocation(level, feet)) {
                return Optional.of(feet);
            }
        }

        return Optional.empty();
    }
}
