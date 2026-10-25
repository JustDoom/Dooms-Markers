package com.imjustdoom.doomsmarkers.command;

import com.google.common.base.Stopwatch;
import com.imjustdoom.doomsmarkers.*;
import com.imjustdoom.doomsmarkers.command.argument.MarkerArgument;
import com.imjustdoom.doomsmarkers.command.argument.MarkerLiteArgument;
import com.imjustdoom.doomsmarkers.util.PositionUtil;
import com.imjustdoom.doomsmarkers.util.ServerUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Commands {
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_NOT_FOUND = new DynamicCommandExceptionType((args) ->
            Component.translatable("commands.locate.structure.not_found", args));
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID = new DynamicCommandExceptionType((args) ->
            Component.translatable("commands.locate.structure.invalid", args));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
                literal("marker")
                        .requires(source -> source.hasPermission(2))
                        .then(argument("player", EntityArgument.player())
                                .then(literal("add")
                                        .then(argument("marker", MarkerArgument.marker(context))
                                                .then(argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                                                        .executes(Commands::markersItem) // All the info
                                                )
                                                .executes(Commands::markersItem) // Specific marker but no structure
                                        )
                                        .executes(Commands::markersItem) // When no marker info is specified. Just use default one
                                )
                                .then(literal("remove")
                                        .then(argument("marker_lite", MarkerLiteArgument.marker(context))
                                                .then(literal("all")
                                                        .executes(ctx -> markersRemove(ctx, false))
                                                )
                                                .then(literal("first")
                                                        .executes(ctx -> markersRemove(ctx, true))
                                                )
                                                .executes(ctx -> markersRemove(ctx, false))
                                        )
                                        .executes(Commands::markersMissingExistingMarker) // When any marker targets are missing
                                )
                                .then(literal("teleport")
                                        .then(argument("marker_lite", MarkerLiteArgument.marker(context))
                                                .then(argument("target_player", EntityArgument.player())
                                                        .executes(Commands::markersTeleport)
                                                )
                                                .then(argument("minimum_range", IntegerArgumentType.integer(0))
                                                        .then(argument("maximum_range", IntegerArgumentType.integer(0))
                                                                .then(argument("attempts", IntegerArgumentType.integer(1))
                                                                        .then(argument("target_player", EntityArgument.player())
                                                                                .executes(Commands::markersTeleport)
                                                                        )
                                                                        .executes(Commands::markersTeleport)
                                                                )
                                                                .then(argument("target_player", EntityArgument.player())
                                                                        .executes(Commands::markersTeleport)
                                                                )
                                                                .executes(Commands::markersTeleport)
                                                        )
                                                )
                                                .executes(Commands::markersTeleport)
                                        )
                                        .executes(Commands::markersMissingExistingMarker) // When any marker targets are missing
                                )
                                .executes(Commands::markersMissingAction) // When an action is not specified
                        )
                        .executes(Commands::markersMissingPlayer) // When no player is specified
        );
    }

    private static int markersItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer serverPlayer = EntityArgument.getPlayer(context, "player");
        ServerPlayerInterface serverPlayerLayer = (ServerPlayerInterface) serverPlayer;

        // Handle marker limits
        if (serverPlayerLayer.getMarkers().size() >= Config.get().maxMarkers) {
            context.getSource().sendFailure(Component.literal(serverPlayer.getName().getString() + " is at the max of " + Config.get().maxMarkers + " markers :("));
            return 1;
        }

        // Ensure marker is included otherwise default
        Marker marker;
        try {
            marker = MarkerArgument.getMarker(context, "marker");
        } catch (IllegalArgumentException exception) {
            marker = new Marker("custom");
        }

        // Handle location
        try {
            marker.setPosition(markersStructure(context));
        } catch (IllegalArgumentException exception1) {
            if (marker.getPosition() == null) {
                marker.setPosition(serverPlayer.position().add(0, 0.75f, 0));
            }
        }

        if (marker.getDimension() == null) {
            marker.setDimension(serverPlayer.serverLevel().dimension());
        }

        // Save marker and send it
        serverPlayerLayer.getMarkers().add(marker);
        ServerUtil.sendMarkerToPlayer(serverPlayer, marker);

        String message = String.format("Successfully created the marker \"%s\" for player %s", marker, serverPlayer.getName().getString());
        DoomsMarkers.LOG.info(message);
        context.getSource().sendSuccess(() -> Component.literal(message), false);

        return 1;
    }

    private static Vec3 markersStructure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceOrTagKeyArgument.Result<Structure> structure = ResourceOrTagKeyArgument.getResourceOrTagKey(context, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID);
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> holder = getHolders(structure, registry).orElseThrow(() -> ERROR_STRUCTURE_INVALID.create(structure.asPrintable()));
        BlockPos blockPos = BlockPos.containing(source.getPosition());
        Stopwatch timer = Stopwatch.createStarted(Util.TICKER);
        Pair<BlockPos, Holder<Structure>> pair = level.getChunkSource().getGenerator().findNearestMapStructure(level, holder, blockPos, 100, false);
        timer.stop();
        if (pair == null) {
            throw ERROR_STRUCTURE_NOT_FOUND.create(structure.asPrintable());
        } else {
            DoomsMarkers.LOG.info("Locating element {} took {} ms",
                    structure.unwrap().map((key) -> key.location().toString(), (tag) -> "#" + tag.location() + " (" +
                            pair.getSecond().unwrapKey().map((key) -> key.location().toString()).orElse("[unregistered]") + ")"),
                    timer.elapsed().toMillis());

            BlockPos found = pair.getFirst();
            ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(found.getX()),
                    SectionPos.blockToSectionCoord(found.getZ()), ChunkStatus.STRUCTURE_STARTS);
            StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), pair.getSecond().value(), chunk);
            boolean hasStart = start != null && start.isValid();
            BlockPos marker = hasStart ? start.getBoundingBox().getCenter() : found;

            Vec3 pos = marker.getCenter();
            return new Vec3(pos.x, hasStart ? pos.y : 69, pos.z);
        }
    }

    private static int markersTeleport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // The player who owns the marker
        ServerPlayer markerOwner = EntityArgument.getPlayer(context, "player");
        ServerPlayerInterface markerOwnerInterface = (ServerPlayerInterface) markerOwner;
        CommandSourceStack source = context.getSource();

        // Get the target player to teleport to the marker. Throws if not specified which is then handled to just
        // teleport the sender to it instead
        ServerPlayer targetPlayer;
        try {
            targetPlayer = EntityArgument.getPlayer(context, "target_player");
        } catch (IllegalArgumentException exception) {
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Must be player to use this command"));
                return 1;
            }

            targetPlayer = source.getPlayer();
        }

        // Find the first matching marker. Does not find by closest distance
        final Set<Marker> markers = getMatchingMarkers(context, markerOwnerInterface, true);
        if (markers == null) {
            return 1;
        }

        if (markers.isEmpty()) {
            source.sendFailure(Component.literal("Unable to find a matching marker :("));
            return 1;
        }

        final Marker marker = markers.stream().findFirst().get();

        // Get the dimension to teleport to
        ServerLevel level = source.getServer().getLevel(marker.getDimension());
        if (level == null) {
            source.sendFailure(Component.literal("Unable to find a matching dimension"));
            return 1;
        }

        Integer min = null;
        Integer max = null;
        int attempts = 10;
        try {
            min = IntegerArgumentType.getInteger(context, "minimum_range");
            max = IntegerArgumentType.getInteger(context, "maximum_range");
            attempts = IntegerArgumentType.getInteger(context, "attempts");
        } catch (IllegalArgumentException ignored) {}

        // Handle the range teleport option
        if (min != null && max != null) {
            if (min > max) {
                source.sendFailure(Component.literal("minimum_range can not be bigger than maximum_range"));
                return 1;
            }

            Optional<BlockPos> optionalBlockPos = PositionUtil.findSafeTeleportPosition(level, marker.getPosition(), min, max, attempts);
            if (optionalBlockPos.isEmpty()) {
                source.sendFailure(Component.literal("Unable to find suitable position to teleport to"));
                return 1;
            }

            BlockPos blockPos = optionalBlockPos.get();
            targetPlayer.teleportTo(level,
                    blockPos.getX() + 0.5f,
                    blockPos.getY(),
                    blockPos.getZ() + 0.5f,
                    targetPlayer.getYRot(),
                    targetPlayer.getXRot());

            String message = String.format("Successfully teleported %s to the marker \"%s\" for player %s", targetPlayer.getName().getString(), marker, markerOwner.getName().getString());
            DoomsMarkers.LOG.info(message);
            source.sendSuccess(() -> Component.literal(message), false);

            return 1;
        }

        BlockPos blockPos = new BlockPos(
                (int) Math.floor(marker.getPosition().x()),
                (int) Math.floor(marker.getPosition().y() - 0.75f),
                (int) Math.floor(marker.getPosition().z()));

        if (!PositionUtil.isSafeTeleportLocation(level, blockPos)) {
            source.sendFailure(Component.literal("The Marker does not seem to be safe to teleport to"));
            return 1;
        }

        targetPlayer.teleportTo(level,
                blockPos.getX() + 0.5f,
                blockPos.getY(),
                blockPos.getZ() + 0.5f,
                targetPlayer.getYRot(),
                targetPlayer.getXRot());

        String message = String.format("Successfully teleported %s to the marker \"%s\" for player %s", targetPlayer.getName().getString(), marker, markerOwner.getName().getString());
        DoomsMarkers.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);

        return 1;
    }

    private static int markersRemove(CommandContext<CommandSourceStack> context, boolean first) throws CommandSyntaxException {
        ServerPlayer serverPlayer = EntityArgument.getPlayer(context, "player");
        ServerPlayerInterface serverPlayerLayer = (ServerPlayerInterface) serverPlayer;

        Set<Marker> markers = getMatchingMarkers(context, serverPlayerLayer, first);

        if (markers == null) {
            return 1;
        }

        if (markers.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Unable to find a matching marker :("));
            return 1;
        }

        for (Marker marker : markers) {
            serverPlayerLayer.getMarkers().remove(marker);
            ServerUtil.removeMarkerFromPlayer(serverPlayer, marker);
        }

        String message = String.format("Successfully removed %d markers for player %s", markers.size(), serverPlayer.getName().getString());
        DoomsMarkers.LOG.info(message);
        context.getSource().sendSuccess(() -> Component.literal(message), false);

        return 1;
    }

    /**
     * Finds all matches of the markers if specified. Returns null on error or empty list for no matches
     * @param context
     * @param serverPlayerLayer
     * @return
     */
    private static Set<Marker> getMatchingMarkers(CommandContext<CommandSourceStack> context, ServerPlayerInterface serverPlayerLayer, boolean first) {
        // Get the first matching marker
        MarkerLite markerLite;
        try {
            markerLite = MarkerLiteArgument.getMarker(context, "marker_lite");
        } catch (IllegalArgumentException exception) {
            context.getSource().sendFailure(Component.literal("Unable to validate marker specifications"));
            return null;
        }

        Set<Marker> markers = new HashSet<>();
        for (Marker marker : serverPlayerLayer.getMarkers()) {
            if ((markerLite.uuid() == null || markerLite.uuid().equals(marker.getUuid()))
                    && (markerLite.type() == null || markerLite.type().equals(marker.getType()))
                    && (markerLite.position() == null || markerLite.position().equals(marker.getPosition()) || markerLite.position().add(0, 0.75f, 0).equals(marker.getPosition()))
                    && (markerLite.dimension() == null || markerLite.dimension().equals(marker.getDimension()))) {
                markers.add(marker);
                if (first) {
                    break;
                }
            }
        }

        return markers;
    }

    private static int markersMissingPlayer(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Please specify a player to apply the marks to"));
        return 1;
    }

    private static int markersMissingAction(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Please specify an action to take"));
        return 1;
    }

    private static int markersMissingExistingMarker(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Please specify a marker to affect"));
        return 1;
    }

    private static Optional<? extends HolderSet.ListBacked<Structure>> getHolders(ResourceOrTagKeyArgument.Result<Structure> structure, Registry<Structure> structureRegistry) {
        return structure.unwrap().map((key) -> structureRegistry.getHolder(key).map(HolderSet::direct), structureRegistry::getTag);
    }

}
