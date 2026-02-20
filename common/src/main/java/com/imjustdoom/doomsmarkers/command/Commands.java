package com.imjustdoom.doomsmarkers.command;

import com.google.common.base.Stopwatch;
import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.ServerPlayerInterface;
import com.imjustdoom.doomsmarkers.command.argument.MarkerArgument;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
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

import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Commands {
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_NOT_FOUND = new DynamicCommandExceptionType((args) ->
            Component.translatable("commands.locate.structure.not_found", args));
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID = new DynamicCommandExceptionType((args) ->
            Component.translatable("commands.locate.structure.invalid", args));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("marker")
                        .requires(source -> source.hasPermission(2))
                        .then(argument("player", EntityArgument.player())
//                                .then(literal("teleport")
//                                )
                                .then(literal("add")
                                        .then(argument("marker", MarkerArgument.marker())
                                                .then(argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                                                        .executes(Commands::markersItem) // All the info
                                                )
                                                .executes(Commands::markersItem) // Specific marker but no structure
                                        )
                                        .executes(Commands::markersItem) // When no marker info is specified. Just use default one
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
        if (serverPlayerLayer.getMarkers().size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
            context.getSource().sendFailure(Component.literal(serverPlayer.getName().getString() + " is at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :("));
            return 1;
        }

        // Ensure marker is included otherwise default
        Marker marker;
        try {
            marker = MarkerArgument.getMarker(context, "marker");
        } catch (IllegalArgumentException exception) {
            marker = new Marker();
        }

        // Handle location
        try {
            marker.setPosition(markersStructure(context));
        } catch (IllegalArgumentException exception1) {
            if (marker.getPosition() == null) {
                marker.setPosition(serverPlayer.position().add(0, 0.75f, 0));
            }
        }

        // Save marker and send it
        serverPlayerLayer.getMarkers().add(marker);
        DoomsMarkers.sendMarkerToPlayer(serverPlayer, marker);

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

    private static int markersMissingPlayer(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Please specify a player to apply the marks to"));
        return 1;
    }

    private static int markersMissingAction(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Please specify an action to take"));
        return 1;
    }

    private static Optional<? extends HolderSet.ListBacked<Structure>> getHolders(ResourceOrTagKeyArgument.Result<Structure> structure, Registry<Structure> structureRegistry) {
        return structure.unwrap().map((key) -> structureRegistry.getHolder(key).map(HolderSet::direct), structureRegistry::getTag);
    }
}
