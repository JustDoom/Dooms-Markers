package com.imjustdoom.doomsmarkers.command.argument;

import com.imjustdoom.doomsmarkers.MarkerLite;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;

import java.util.concurrent.CompletableFuture;

public class MarkerLiteArgument implements ArgumentType<MarkerLite> {
    public static MarkerLiteArgument marker(CommandBuildContext context) {
        return new MarkerLiteArgument(context);
    }

    private final CommandBuildContext context;

    public MarkerLiteArgument(CommandBuildContext context) {
        this.context = context;
    }

    @Override
    public MarkerLite parse(StringReader reader) throws CommandSyntaxException {
        return MarkerLiteParser.parse(reader, this.context);
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

    public static MarkerLite getMarker(CommandContext<?> context, String name) {
        return context.getArgument(name, MarkerLite.class);
    }
}