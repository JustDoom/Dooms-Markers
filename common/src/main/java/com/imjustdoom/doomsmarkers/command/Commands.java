package com.imjustdoom.doomsmarkers.command;

import com.google.common.base.Stopwatch;
import com.imjustdoom.doomsmarkers.*;
import com.imjustdoom.doomsmarkers.command.argument.MarkerArgument;
import com.imjustdoom.doomsmarkers.command.argument.MarkerLiteArgument;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
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
        DoomsMarkers.sendMarkerToPlayer(serverPlayer, marker);

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
            Vec3 pos = pair.getFirst().getCenter();
            return new Vec3(pos.x, 69, pos.z);
        }
    }

    private static int markersTeleport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer serverPlayer = EntityArgument.getPlayer(context, "player");
        ServerPlayerInterface serverPlayerLayer = (ServerPlayerInterface) serverPlayer;

        ServerPlayer targetPlayer;
        try {
            targetPlayer = EntityArgument.getPlayer(context, "target_player");
        } catch (IllegalArgumentException exception) {
            if (!context.getSource().isPlayer()) {
                context.getSource().sendFailure(Component.literal("Must be player to use this command"));
                return 1;
            }

            targetPlayer = context.getSource().getPlayer();
        }

        Set<Marker> markers = getMatchingMarkers(context, serverPlayerLayer, true);

        if (markers == null) {
            return 1;
        }

        if (markers.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Unable to find a matching marker :("));
            return 1;
        }

        Marker marker = markers.stream().findFirst().get();

        ServerLevel level = context.getSource().getServer().getLevel(marker.getDimension());
        if (level == null) {
            context.getSource().sendFailure(Component.literal("Unable to find a matching dimension"));
            return 1;
        }

        targetPlayer.teleportTo(level,
                marker.getPosition().x(),
                marker.getPosition().y(),
                marker.getPosition().z(),
                targetPlayer.getYRot(),
                targetPlayer.getXRot());

        String message = String.format("Successfully teleported %s to the marker \"%s\" for player %s", targetPlayer.getName().getString(), marker, serverPlayer.getName().getString());
        DoomsMarkers.LOG.info(message);
        context.getSource().sendSuccess(() -> Component.literal(message), false);

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
            DoomsMarkers.removeMarkerFromPlayer(serverPlayer, marker);
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
     * @throws CommandSyntaxException
     */
    private static Set<Marker> getMatchingMarkers(CommandContext<CommandSourceStack> context, ServerPlayerInterface serverPlayerLayer, boolean first) throws CommandSyntaxException {
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
            if ((markerLite.getUuid() == null || markerLite.getUuid().equals(marker.getUuid()))
                    && (markerLite.getType() == null || markerLite.getType().equals(marker.getType()))
                    && (markerLite.getPosition() == null || markerLite.getPosition().equals(marker.getPosition()))
                    && (markerLite.getDimension() == null || markerLite.getDimension().equals(marker.getDimension()))) {
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
