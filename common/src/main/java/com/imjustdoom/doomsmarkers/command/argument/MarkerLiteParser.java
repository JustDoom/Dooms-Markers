package com.imjustdoom.doomsmarkers.command.argument;

import com.imjustdoom.doomsmarkers.MarkerLite;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

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
    private Coordinates position;
    private ResourceKey<Level> dimension;

    public MarkerLiteParser(StringReader reader, CommandBuildContext context) {
        this.reader = reader;
        this.commandBuildContext = context;
    }

    private MarkerLiteParser parse() throws CommandSyntaxException {
        this.reader.skipWhitespace();

        if (this.reader.canRead() && this.reader.peek() == '[') {
            this.reader.skip();
            parseProperties();
            this.reader.skipWhitespace();

            if (!this.reader.canRead() || this.reader.read() != ']') {
                throw ERROR_EXPECTED_CLOSE_BRACKET.createWithContext(this.reader);
            }
        }

        return this;
    }

    public MarkerLite toMarkerLite(CommandSourceStack source) {
        return new MarkerLite(this.uuid, this.type, this.position == null ? null : this.position.getPosition(source), this.dimension);
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
            case "pos", "position" -> this.position = parseCoordinates();
            case "dim", "dimension" -> this.dimension = parseDimension();
            default -> throw ERROR_UNKNOWN_PROPERTY.create(key);
        }
    }

    private Coordinates parseCoordinates() throws CommandSyntaxException {
        WorldCoordinate x = parseCoordinate();
        expectSeparator();
        WorldCoordinate y = parseCoordinate();
        expectSeparator();
        WorldCoordinate z = parseCoordinate();
        return new WorldCoordinates(x, y, z);
    }

    private WorldCoordinate parseCoordinate() throws CommandSyntaxException {
        if (!this.reader.canRead() || this.reader.peek() != '~') {
            return new WorldCoordinate(false, parseFloat());
        }

        this.reader.skip();
        boolean hasOffset = this.reader.canRead() && isNumberChar(this.reader.peek());
        return new WorldCoordinate(true, hasOffset ? parseFloat() : 0);
    }

    private void expectSeparator() throws CommandSyntaxException {
        boolean separated = this.reader.canRead() && Character.isWhitespace(this.reader.peek());
        this.reader.skipWhitespace();

        if (this.reader.canRead() && this.reader.peek() == ',') {
            this.reader.skip();
            this.reader.skipWhitespace();
            separated = true;
        }

        if (!separated) {
            throw new SimpleCommandExceptionType(Component.literal("Expected ','")).createWithContext(this.reader);
        }
    }

    private float parseFloat() throws CommandSyntaxException {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && isNumberChar(this.reader.peek())) {
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

    private boolean isNumberChar(char c) {
        return Character.isDigit(c) || c == '.' || c == '-';
    }

    public static MarkerLiteParser parse(StringReader reader, CommandBuildContext context) throws CommandSyntaxException {
        return new MarkerLiteParser(reader, context).parse();
    }
}