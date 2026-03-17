package com.imjustdoom.doomsmarkers;

import java.util.List;

public class ColourUtil {
    public static List<Float> argbIntToFloatList(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return List.of(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    public static int floatListToArgbInt(List<Float> color) {
        int r = (int) (color.get(0) * 255);
        int g = (int) (color.get(1) * 255);
        int b = (int) (color.get(2) * 255);
        int a = (int) (color.get(3) * 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int floatListToRgbInt(List<Float> color) {
        int r = (int) (color.get(0) * 255);
        int g = (int) (color.get(1) * 255);
        int b = (int) (color.get(2) * 255);

        return (r << 16) | (g << 8) | b;
    }
}
