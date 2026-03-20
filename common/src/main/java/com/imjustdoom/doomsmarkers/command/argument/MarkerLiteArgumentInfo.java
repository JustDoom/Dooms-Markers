package com.imjustdoom.doomsmarkers.command.argument;

import com.google.gson.JsonObject;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class MarkerLiteArgumentInfo implements ArgumentTypeInfo<MarkerLiteArgument, MarkerLiteArgumentInfo.Template> {
    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {}

    @Override
    public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
        return new Template();
    }

    @Override
    public void serializeToJson(Template template, JsonObject json) {}

    @Override
    public Template unpack(MarkerLiteArgument argument) {
        return new Template();
    }

    public final class Template implements ArgumentTypeInfo.Template<MarkerLiteArgument> {
        @Override
        public MarkerLiteArgument instantiate(CommandBuildContext context) {
            return new MarkerLiteArgument(context);
        }

        @Override
        public ArgumentTypeInfo<MarkerLiteArgument, ?> type() {
            return MarkerLiteArgumentInfo.this;
        }
    }
}