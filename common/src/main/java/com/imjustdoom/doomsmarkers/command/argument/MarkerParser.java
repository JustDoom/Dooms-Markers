package com.imjustdoom.doomsmarkers.command.argument;

import com.imjustdoom.doomsmarkers.Marker;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
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
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_DIMENSION =
            new DynamicCommandExceptionType(dimension -> Component.literal("Unknown dimension: " + dimension));

    private final StringReader reader;
    private final CommandBuildContext commandBuildContext;

    private Vec3 position;
    private List<Float> colour = List.of(1.0f, 1.0f, 1.0f);
    private int iconIndex = 0;
    private Item itemIcon = Items.AIR;
    private ResourceKey<Level> dimension; // Leave blank because it will default to the players dimension if null
    private boolean canPlayerRemove = true;
    private boolean canPlayerCustomise = true;
    private int removeWhenNearby = -1;

    public MarkerParser(StringReader reader, CommandBuildContext context) {
        this.reader = reader;
        this.commandBuildContext = context;
    }

    private Marker parse() throws CommandSyntaxException {
        this.reader.skipWhitespace();

        if (this.reader.canRead() && this.reader.peek() == '[') {
            this.reader.skip();
            parseProperties();
            this.reader.skipWhitespace();

            if (!this.reader.canRead() || this.reader.read() != ']') {
                throw ERROR_EXPECTED_CLOSE_BRACKET.createWithContext(this.reader);
            }
        }

        return new Marker(this.position, this.colour, this.iconIndex, this.itemIcon, this.dimension, this.canPlayerRemove, this.canPlayerCustomise, this.removeWhenNearby);
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
            case "pos", "position" -> this.position = parseVec3();
            case "color", "colour" -> this.colour = parseColorList();
            case "icon", "iconindex" -> this.iconIndex = parseInt();
            case "item" -> {
                this.iconIndex = -1;
                this.itemIcon = parseItem();
            }
            case "dim", "dimension" -> this.dimension = parseDimension();
            case "canplayerremove", "playerremove", "remove" -> this.canPlayerRemove = parseBoolean();
            case "canplayercustomise", "playercustomise", "customise" -> this.canPlayerCustomise = parseBoolean();
            case "removewhennearby" -> this.removeWhenNearby = parseInt();
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

    private List<Float> parseColorList() throws CommandSyntaxException {
        float r = parseFloat();
        expectComma();
        float g = parseFloat();
        expectComma();
        float b = parseFloat();
        return List.of(r, g, b);
    }

    private void expectComma() throws CommandSyntaxException {
        this.reader.skipWhitespace();
        if (!this.reader.canRead() || this.reader.read() != ',') {
            throw new SimpleCommandExceptionType(Component.literal("Expected ','")).createWithContext(this.reader);
        }
        this.reader.skipWhitespace();
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

    private boolean parseBoolean() {
        int start = this.reader.getCursor();
        while (this.reader.canRead() && isItemChar(this.reader.peek())) {
            this.reader.skip();
        }

        return Boolean.parseBoolean(this.reader.getString().substring(start, this.reader.getCursor()));
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

    public static Marker parse(StringReader reader, CommandBuildContext context) throws CommandSyntaxException {
        return new MarkerParser(reader, context).parse();
    }
}