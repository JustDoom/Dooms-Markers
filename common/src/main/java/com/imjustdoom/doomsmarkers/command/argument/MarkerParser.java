package com.imjustdoom.doomsmarkers.command.argument;

import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.Markers;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MarkerParser {
    private static final SimpleCommandExceptionType ERROR_EXPECTED_OPEN_BRACKET =
            new SimpleCommandExceptionType(Component.literal("Expected '['"));
    private static final SimpleCommandExceptionType ERROR_EXPECTED_CLOSE_BRACKET =
            new SimpleCommandExceptionType(Component.literal("Expected ']'"));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PROPERTY =
            new DynamicCommandExceptionType(prop -> Component.literal("Unknown property: " + prop));
    private static final SimpleCommandExceptionType ERROR_INVALID_NUMBER =
            new SimpleCommandExceptionType(Component.literal("Invalid number"));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM =
            new DynamicCommandExceptionType(item -> Component.literal("Unknown item: " + item));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ICON =
            new DynamicCommandExceptionType(icon -> Component.literal("Unknown icon: " + icon));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_DIMENSION =
            new DynamicCommandExceptionType(dimension -> Component.literal("Unknown dimension: " + dimension));

    private final StringReader reader;
    private final CommandBuildContext commandBuildContext;

    private String type = "custom";
    private Coordinates position;
    private List<Float> colour = List.of(1.0f, 1.0f, 1.0f);
    private String icon = Markers.DOT.name();
    private Item itemIcon = Items.AIR;
    private ResourceKey<Level> dimension; // Leave blank because it will default to the players dimension if null
    private boolean canPlayerRemove = true;
    private boolean canPlayerCustomise = true;
    private int removeWhenNearby = -1;

    public MarkerParser(StringReader reader, CommandBuildContext context) {
        this.reader = reader;
        this.commandBuildContext = context;
    }

    private MarkerParser parse() throws CommandSyntaxException {
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

    public Marker toMarker(CommandSourceStack source) {
        Vec3 pos = this.position == null ? null : this.position.getPosition(source);
        return new Marker(this.type, pos, this.colour, Markers.getIconByName(this.icon), this.itemIcon, this.dimension, this.canPlayerRemove, this.canPlayerCustomise, this.removeWhenNearby);
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
            case "type" -> this.type = parseString();
            case "pos", "position" -> this.position = parseCoordinates();
            case "color", "colour" -> this.colour = parseColorList();
            case "icon", "iconindex" -> this.icon = parseIcon();
            case "item" -> {
                this.icon = Markers.CUSTOM_ITEM.name();
                this.itemIcon = parseItem();
            }
            case "dim", "dimension" -> this.dimension = parseDimension();
            case "canplayerremove", "playerremove", "remove" -> this.canPlayerRemove = parseBoolean();
            case "canplayercustomise", "playercustomise", "customise" -> this.canPlayerCustomise = parseBoolean();
            case "removewhennearby" -> this.removeWhenNearby = parseInt();
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

    private List<Float> parseColorList() throws CommandSyntaxException {
        float r = parseFloat();
        expectSeparator();
        float g = parseFloat();
        expectSeparator();
        float b = parseFloat();
        return List.of(r, g, b);
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

    private int parseInt() throws CommandSyntaxException {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && (Character.isDigit(this.reader.peek()) || this.reader.peek() == '-')) {
            this.reader.skip();
        }

        String number = this.reader.getString().substring(start, this.reader.getCursor());
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            this.reader.setCursor(start);
            throw ERROR_INVALID_NUMBER.createWithContext(this.reader);
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

    private boolean parseBoolean() {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && isItemChar(this.reader.peek())) {
            this.reader.skip();
        }

        return Boolean.parseBoolean(this.reader.getString().substring(start, this.reader.getCursor()));
    }

    private String parseString() throws CommandSyntaxException {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && isItemChar(this.reader.peek())) {
            this.reader.skip();
        }

        return this.reader.getString().substring(start, this.reader.getCursor());
    }

    private String parseIcon() throws CommandSyntaxException {
        final String icon = parseString();
        if (Markers.getIconByName(icon) == null) {
            throw ERROR_UNKNOWN_ICON.createWithContext(this.reader, icon);
        }
        return icon;
    }

    private Item parseItem() throws CommandSyntaxException {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && isItemChar(this.reader.peek())) {
            this.reader.skip();
        }

        String itemId = this.reader.getString().substring(start, this.reader.getCursor());
        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);

        if (resourceLocation == null || !BuiltInRegistries.ITEM.containsKey(resourceLocation)) {
            this.reader.setCursor(start);
            throw ERROR_UNKNOWN_ITEM.createWithContext(this.reader, itemId);
        }

        return BuiltInRegistries.ITEM.get(resourceLocation);
    }

    public ResourceKey<Level> parseDimension() throws CommandSyntaxException {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && isItemChar(this.reader.peek())) {
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

    private boolean isItemChar(char c) {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == ':' || c == '.';
    }

    private boolean isNumberChar(char c) {
        return Character.isDigit(c) || c == '.' || c == '-';
    }

    public static MarkerParser parse(StringReader reader, CommandBuildContext context) throws CommandSyntaxException {
        return new MarkerParser(reader, context).parse();
    }
}