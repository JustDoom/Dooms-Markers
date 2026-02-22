package com.imjustdoom.doomsmarkers.command.argument;

import com.imjustdoom.doomsmarkers.Marker;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;

import java.util.concurrent.CompletableFuture;

public class MarkerArgument implements ArgumentType<Marker> {
    public static MarkerArgument marker(CommandBuildContext context) {
        return new MarkerArgument(context);
    }

    private final CommandBuildContext context;

    public MarkerArgument(CommandBuildContext context) {
        this.context = context;
    }

    @Override
    public Marker parse(StringReader reader) throws CommandSyntaxException {
        return MarkerParser.parse(reader, this.context);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest("[pos=0,64,0]");
            builder.suggest("[pos=0,64,0,color=1.0,0.0,0.0]");
            builder.suggest("[pos=0,64,0,icon=0]");
            builder.suggest("[pos=0,64,0,icon=0,dimension=minecraft:overworld]");
        }
        return builder.buildFuture();
    }

    public static Marker getMarker(CommandContext<?> context, String name) {
        return context.getArgument(name, Marker.class);
    }
}