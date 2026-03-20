package com.imjustdoom.doomsmarkers.command.argument;

import com.imjustdoom.doomsmarkers.MarkerLite;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class MarkerLiteParser {
    private static final SimpleCommandExceptionType ERROR_EXPECTED_OPEN_BRACKET =
            new SimpleCommandExceptionType(Component.literal("Expected '['"));
    private static final SimpleCommandExceptionType ERROR_EXPECTED_CLOSE_BRACKET =
            new SimpleCommandExceptionType(Component.literal("Expected ']'"));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PROPERTY =
            new DynamicCommandExceptionType(prop -> Component.literal("Unknown property: " + prop));
    private static final SimpleCommandExceptionType ERROR_INVALID_NUMBER =
            new SimpleCommandExceptionType(Component.literal("Invalid number"));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_DIMENSION =
            new DynamicCommandExceptionType(dimension -> Component.literal("Unknown dimension: " + dimension));

    private final StringReader reader;
    private final CommandBuildContext commandBuildContext;

    private UUID uuid;
    private String type;
    private Vec3 position;
    private ResourceKey<Level> dimension;

    public MarkerLiteParser(StringReader reader, CommandBuildContext context) {
        this.reader = reader;
        this.commandBuildContext = context;
    }

    private MarkerLite parse() throws CommandSyntaxException {
        this.reader.skipWhitespace();

        if (this.reader.canRead() && this.reader.peek() == '[') {
            this.reader.skip();
            parseProperties();
            this.reader.skipWhitespace();

            if (!this.reader.canRead() || this.reader.read() != ']') {
                throw ERROR_EXPECTED_CLOSE_BRACKET.createWithContext(this.reader);
            }
        }

        return new MarkerLite(this.uuid, this.type, this.position, this.dimension);
    }

    private void parseProperties() throws CommandSyntaxException {
        this.reader.skipWhitespace();
        while (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            String key = readKey();
            this.reader.skipWhitespace();

            if (!this.reader.canRead() || this.reader.read() != '=') {
                throw new SimpleCommandExceptionType(Component.literal("Expected '=' after property name")).createWithContext(this.reader);
            }

            this.reader.skipWhitespace();
            parseValue(key);
            this.reader.skipWhitespace();

            if (this.reader.canRead() && this.reader.peek() == ',') {
                this.reader.skip();
            }
        }
    }

    private String readKey() throws CommandSyntaxException {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && isKeyChar(this.reader.peek())) {
            this.reader.skip();
        }

        if (start == this.reader.getCursor()) {
            throw new SimpleCommandExceptionType(Component.literal("Expected property name")).createWithContext(this.reader);
        }

        return this.reader.getString().substring(start, this.reader.getCursor());
    }

    private boolean isKeyChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    private void parseValue(String key) throws CommandSyntaxException {
        switch (key.toLowerCase()) {
            case "uuid", "id" -> this.uuid = UUID.fromString(parseString());
            case "type" -> this.type = parseString();
            case "pos", "position" -> this.position = parseVec3();
            case "dim", "dimension" -> this.dimension = parseDimension();
            default -> throw ERROR_UNKNOWN_PROPERTY.create(key);
        }
    }

    private Vec3 parseVec3() throws CommandSyntaxException {
        double x = parseFloat();
        expectComma();
        double y = parseFloat();
        expectComma();
        double z = parseFloat();
        return new Vec3(x, y, z);
    }

    private void expectComma() throws CommandSyntaxException {
        this.reader.skipWhitespace();
        if (!this.reader.canRead() || this.reader.read() != ',') {
            throw new SimpleCommandExceptionType(Component.literal("Expected ','")).createWithContext(this.reader);
        }
        this.reader.skipWhitespace();
    }

    private float parseFloat() throws CommandSyntaxException {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && (Character.isDigit(this.reader.peek()) || this.reader.peek() == '.' || this.reader.peek() == '-')) {
            this.reader.skip();
        }

        String number = this.reader.getString().substring(start, this.reader.getCursor());
        try {
            return Float.parseFloat(number);
        } catch (NumberFormatException e) {
            this.reader.setCursor(start);
            throw ERROR_INVALID_NUMBER.createWithContext(this.reader);
        }
    }

    private String parseString() {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && isValidChar(this.reader.peek())) {
            this.reader.skip();
        }

        return this.reader.getString().substring(start, this.reader.getCursor());
    }

    public ResourceKey<Level> parseDimension() throws CommandSyntaxException {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && isValidChar(this.reader.peek())) {
            this.reader.skip();
        }

        String dimension = this.reader.getString().substring(start, this.reader.getCursor());
        ResourceLocation resourceLocation = ResourceLocation.tryParse(dimension);

        if (resourceLocation == null) {
            this.reader.setCursor(start);
            throw ERROR_UNKNOWN_DIMENSION.createWithContext(this.reader, dimension);
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, resourceLocation);

        try {
            HolderLookup<Level> dimensionRegistry = this.commandBuildContext.holderLookup(Registries.DIMENSION);
            if (dimensionRegistry.get(dimensionKey).isEmpty()) {
                this.reader.setCursor(start);
                throw ERROR_UNKNOWN_DIMENSION.createWithContext(this.reader, dimension);
            }
        } catch (Exception ignored) {}

        return dimensionKey;
    }

    private boolean isValidChar(char c) {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == ':' || c == '.' || c == '-';
    }

    public static MarkerLite parse(StringReader reader, CommandBuildContext context) throws CommandSyntaxException {
        return new MarkerLiteParser(reader, context).parse();
    }
}