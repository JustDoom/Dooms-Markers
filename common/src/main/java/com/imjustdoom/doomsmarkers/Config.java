package com.imjustdoom.doomsmarkers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Config {
    @SerializedName("max_markers")
    public int maxMarkers = 50;

    @SerializedName("minimum_marker_distance")
    public double minimumMarkerDistance = 5;

    @SerializedName("compress_unit")
    public boolean compressUnit = true;

    @SerializedName("death_markers")
    public DeathMarkers deathMarkers = new DeathMarkers();

    @SerializedName("allowed_dimensions")
    public Dimensions dimensions = new Dimensions();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String CONFIG_FILE_NAME = "config/dooms-markers-config.json";
    private static Config INSTANCE;

    public static Config get() {
        if (INSTANCE == null) {
            INSTANCE = loadConfig();
        }
        return INSTANCE;
    }

    private static Config loadConfig() {
        Path configFile = Path.of(CONFIG_FILE_NAME);
        Config defaults = new Config();

        if (Files.exists(configFile)) {
            try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                JsonObject loadedJson = GSON.fromJson(reader, JsonObject.class);
                if (loadedJson != null) {
                    Config config = GSON.fromJson(merge(GSON.toJsonTree(defaults).getAsJsonObject(), loadedJson), Config.class);
                    saveConfig(config);
                    return config;
                }
            } catch (Exception e) {
                DoomsMarkers.LOG.error("Failed to load config file: ", e);
            }
        }

        saveConfig(defaults);
        return defaults;
    }

    private static JsonObject merge(JsonObject defaults, JsonObject loaded) {
        JsonObject merged = new JsonObject();
        for (String key : defaults.keySet()) {
            JsonElement defaultVal = defaults.get(key);
            if (loaded.has(key)) {
                JsonElement loadedVal = loaded.get(key);
                if (loadedVal.isJsonNull()) {
                    merged.add(key, defaultVal);
                } else if (defaultVal.isJsonObject() && loadedVal.isJsonObject()) {
                    merged.add(key, merge(defaultVal.getAsJsonObject(), loadedVal.getAsJsonObject()));
                } else {
                    merged.add(key, loadedVal);
                }
            } else {
                merged.add(key, defaultVal);
            }
        }

        for (String key : loaded.keySet()) {
            if (!merged.has(key)) {
                merged.add(key, loaded.get(key));
            }
        }

        return merged;
    }

    private static void saveConfig(Config config) {
        Path configPath = Paths.get(CONFIG_FILE_NAME);
        Path parentPath = configPath.getParent();

        if (parentPath != null) {
            try {
                Files.createDirectories(parentPath);
            } catch (IOException e) {
                DoomsMarkers.LOG.error("Failed to create config directory: {}", parentPath, e);
                return;
            }
        }

        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Failed to save config file: ", e);
        }
    }

    public static class DeathMarkers {
        @SerializedName("enabled")
        public boolean enabled = true;

        @SerializedName("remove_when_nearby_distance")
        public int distance = 5;
    }

    public static class Dimensions {
        @SerializedName("whitelist")
        public boolean whitelist = false;

        @SerializedName("dimensions")
        public List<String> dimensions = new ArrayList<>();
    }
}
