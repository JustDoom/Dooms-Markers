package com.imjustdoom.doomsmarkers;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Commands {
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_NOT_FOUND = new DynamicCommandExceptionType((args) ->
            Component.translatable("commands.locate.structure.not_found", args));
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID = new DynamicCommandExceptionType((args) ->
            Component.translatable("commands.locate.structure.invalid", args));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(
                literal("marker")
                        .requires(source -> source.hasPermission(2))
                        .then(argument("player", EntityArgument.player())
                                .then(literal("location")
                                        .then(argument("location", Vec3Argument.vec3())
                                                //.then(literal("icon")) TODO icon is command
                                                .then(itemCommandStack(buildContext))
                                                .executes(Commands::markersItem)) // When player and location is specified
                                        .executes(Commands::markersMissingLocation)) // Location is missing
                                .then(literal("structure")
                                        .then(argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                                                .then(itemCommandStack(buildContext))
                                                .executes(Commands::markersItem))
                                        .executes(Commands::markersMissingStructure))
                                .then(itemCommandStack(buildContext)) // Also allow specifying an item straight onto the player
                                .executes(Commands::markersItem))
                        .executes(Commands::markersMissingPlayer) // When no player is specified
        );
    }

    private static Optional<? extends HolderSet.ListBacked<Structure>> getHolders(ResourceOrTagKeyArgument.Result<Structure> structure, Registry<Structure> structureRegistry) {
        return structure.unwrap().map((p_258231_) -> structureRegistry.getHolder(p_258231_).map(HolderSet::direct), structureRegistry::getTag);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> itemCommandStack(CommandBuildContext buildContext) {
        return literal("item")
                .then(argument("item", ItemArgument.item(buildContext))
                        .then(argument("colour", ColorArgument.color())
                                .executes(Commands::markersItem))
                        .executes(Commands::markersItem))
                .executes(context -> {
                    context.getSource().sendFailure(Component.literal("Please specify an item"));
                    return 1;
                });
    }

    private static int markersItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer serverPlayer = EntityArgument.getPlayer(context, "player");
        ServerPlayerInterface serverPlayerLayer = (ServerPlayerInterface) serverPlayer;

        // Handle marker limits
        if (serverPlayerLayer.getMarkers().size() >= DoomsMarkers.MAX_MARKERS_PER_PLAYER) {
            context.getSource().sendFailure(Component.literal(serverPlayer.getName().getString() + " is at the max of " + DoomsMarkers.MAX_MARKERS_PER_PLAYER + " markers :("));
            return 1;
        }

        // Handle the colour
        List<Float> colour;
        try {
            Integer colourValue = ColorArgument.getColor(context, "colour").getColor();
            if (colourValue == null) {
                throw new IllegalArgumentException("Colour is null");
            }
            colour = new ArrayList<>();
            for (float value : DoomsMarkers.argbIntToFloatArray(colourValue)) {
                colour.add(value);
            }
        } catch (IllegalArgumentException exception) {
            colour = List.of(1f, 1f, 1f, 1f);
        }

        // Handle location
        Vec3 location;
        try {
            location = Vec3Argument.getVec3(context, "location");
        } catch (IllegalArgumentException exception) {
            try {
                location = markersStructure(context);
            } catch (IllegalArgumentException exception1) {
                location = serverPlayer.position().add(0, 0.75f, 0);
            }
        }

        // Handle marker and icon
        Marker marker;
        try {
            Item item = ItemArgument.getItem(context, "item").getItem();
            marker = new Marker(location, colour, item);
        } catch (IllegalArgumentException exception) {
            marker = new Marker(location, colour, 1);
        }

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

    private static int markersMissingLocation(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Please specify a location to apply the marks to"));
        return 1;
    }

    private static int markersMissingStructure(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Please specify a structure to apply the marks to"));
        return 1;
    }
}
