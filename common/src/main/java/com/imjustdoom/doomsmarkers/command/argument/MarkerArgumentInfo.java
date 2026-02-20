package com.imjustdoom.doomsmarkers.command.argument;

import com.google.gson.JsonObject;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class MarkerArgumentInfo implements ArgumentTypeInfo<MarkerArgument, MarkerArgumentInfo.Template> {
    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {}

    @Override
    public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
        return new Template();
    }

    @Override
    public void serializeToJson(Template template, JsonObject json) {}

    @Override
    public Template unpack(MarkerArgument argument) {
        return new Template();
    }

    public final class Template implements ArgumentTypeInfo.Template<MarkerArgument> {
        @Override
        public MarkerArgument instantiate(CommandBuildContext context) {
            return new MarkerArgument();
        }

        @Override
        public ArgumentTypeInfo<MarkerArgument, ?> type() {
            return MarkerArgumentInfo.this;
        }
    }
}