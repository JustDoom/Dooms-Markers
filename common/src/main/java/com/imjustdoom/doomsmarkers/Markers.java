package com.imjustdoom.doomsmarkers;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Markers {
    public static final List<MarkerIcon> MARKER_ICONS = new ArrayList<>();
    public static final Map<String, MarkerIcon> MARKER_ICONS_BY_NAME = new HashMap<>();

    public static final MarkerIcon CUSTOM_ITEM = registerMarkerIcon("custom_item"); // Used when a custom item is rendered instead of an icon

    public static final MarkerIcon DOT = registerMarkerIcon("dot");
    public static final MarkerIcon SQUARE = registerMarkerIcon("square");
    public static final MarkerIcon POINT = registerMarkerIcon("point");
    public static final MarkerIcon DOT_OUTLINED = registerMarkerIcon("dot_outlined");
    public static final MarkerIcon SQUARE_OUTLINED = registerMarkerIcon("square_outlined");
    public static final MarkerIcon POINT_OUTLINED = registerMarkerIcon("point_outlined");
    public static final MarkerIcon BOLT = registerMarkerIcon("bolt");
    public static final MarkerIcon HEART = registerMarkerIcon("heart");
    public static final MarkerIcon STAR = registerMarkerIcon("star");
    public static final MarkerIcon HOME = registerMarkerIcon("home");
    public static final MarkerIcon FLAG = registerMarkerIcon("flag");
    public static final MarkerIcon X = registerMarkerIcon("x");
    public static final MarkerIcon CUBE = registerMarkerIcon("cube");
    public static final MarkerIcon DEATH = registerMarkerIcon("death");
    public static final MarkerIcon GRAVE_SMALL = registerMarkerIcon("grave_small");
    public static final MarkerIcon GRAVE_BIG = registerMarkerIcon("grave_big");
    public static final MarkerIcon SUN = registerMarkerIcon("sun");
    public static final MarkerIcon MOON = registerMarkerIcon("moon");
    public static final MarkerIcon SWORD = registerMarkerIcon("sword");
    public static final MarkerIcon PICKAXE = registerMarkerIcon("pickaxe");
    public static final MarkerIcon AXE = registerMarkerIcon("axe");
    public static final MarkerIcon SHOVEL = registerMarkerIcon("shovel");
    public static final MarkerIcon HO = registerMarkerIcon("ho");
    public static final MarkerIcon SKELETON = registerMarkerIcon("skeleton");
    public static final MarkerIcon CREEPER = registerMarkerIcon("creeper");
    public static final MarkerIcon ZOMBIE = registerMarkerIcon("zombie");
    public static final MarkerIcon PORTAL = registerMarkerIcon("portal");
    public static final MarkerIcon EYE_OF_ENDER = registerMarkerIcon("eye_of_ender");

    public static MarkerIcon registerMarkerIcon(@NotNull final String name) {
        MarkerIcon marker = new MarkerIcon(name);
        MARKER_ICONS_BY_NAME.put(marker.name(), marker);
        MARKER_ICONS.add(marker);
        return marker;
    }

    public static MarkerIcon getIconByName(String name) {
        return MARKER_ICONS_BY_NAME.get(name);
    }
}
