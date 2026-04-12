package com.imjustdoom.doomsmarkers;

import com.imjustdoom.doomsmarkers.payload.ServerboundAddMarkerPayload;
import com.imjustdoom.doomsmarkers.payload.ServerboundDeleteMarkerPayload;
import com.imjustdoom.doomsmarkers.payload.ServerboundUpdateMarkerPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeItem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class DoomsMarkersClient {
    private static final KeyMapping.Category DOOMS_MARKERS_CATEGORY = new KeyMapping.Category(ResourceLocation.parse("key.categories.doomsmarkers"));
    public static final KeyMapping MARKER_KEY_MAPPING = new KeyMapping(
            "category.doomsmarkers.use",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            DOOMS_MARKERS_CATEGORY);
    public static final KeyMapping TOGGLE_MARKER_KEY_MAPPING = new KeyMapping(
            "category.doomsmarkers.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            DOOMS_MARKERS_CATEGORY);

    public static final List<Marker> FOCUSED_MARKERS = new ArrayList<>();
    public static final List<Marker> MARKERS = new ArrayList<>();

    public static boolean KEY_USED_THIS_HOLD = false;
    public static boolean TOGGLED_MARKERS = true; // True displays them

    public static void renderMarkers(GuiGraphics context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        FOCUSED_MARKERS.clear();

        if (!TOGGLED_MARKERS) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vector3f cameraPos = camera.getPosition().toVector3f();

        Matrix4f modelView = new Matrix4f()
                .rotate(Axis.XP.rotationDegrees(camera.getXRot()))
                .rotate(Axis.YP.rotationDegrees(camera.getYRot() + 180.0f))
                .translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        float fov = minecraft.options.fov().get(); // * minecraft.player.getFieldOfViewModifier();
        Matrix4f projectionMatrix = minecraft.gameRenderer.getProjectionMatrix(fov);

        for (Marker marker : new ArrayList<>(DoomsMarkersClient.MARKERS)) {
            Vector4d clipPos = new Vector4d(marker.getPosition().x, marker.getPosition().y, marker.getPosition().z, 1.0f);
            clipPos.mul(modelView);
            clipPos.mul(projectionMatrix);

            if (clipPos.w < 0.0f) {
                continue;
            }

            float screenX = (float) ((context.guiWidth() / 2.0f) * (1.0f + clipPos.x / clipPos.w));
            float screenY = (float) ((context.guiHeight() / 2.0f) * (1.0f - clipPos.y / clipPos.w));

            double distance = Math.sqrt(minecraft.player.distanceToSqr(marker.getPosition().x, marker.getPosition().y, marker.getPosition().z));
            String distanceText = String.format("%.0fm", distance);

            float scale = 1f;
            if (distance <= 10) {
                scale = 1f + (1f - (float) (distance / 10));
            }

            // Don't render if off screen
            float size = 16 * scale;
            if (screenX < -size || screenX > context.guiWidth() + size || screenY < -size || screenY > context.guiHeight() + size) {
                continue;
            }

            // Check if the crosshair is over a marker
            float focusArea = 16 * scale / 1.5f;
            boolean focused = screenX > context.guiWidth() / 2f - focusArea && screenX < context.guiWidth() / 2f + focusArea
                    && screenY > context.guiHeight() / 2f - focusArea && screenY < context.guiHeight() / 2f + focusArea;

            if (focused) {
                FOCUSED_MARKERS.add(marker);
                if (MARKER_KEY_MAPPING.isDown()) {
                    if (minecraft.options.keyAttack.consumeClick()) {
                        DoomsMarkersClient.MARKERS.remove(marker);
                        KEY_USED_THIS_HOLD = true;
                        minecraft.player.connection.send(new ServerboundCustomPayloadPacket(new ServerboundDeleteMarkerPayload(marker.getUuid())));
                    } else if (minecraft.options.keyPickItem.consumeClick()) {
                        marker.setIconIndex(-1);
                        marker.setItemIcon(minecraft.player.getItemInHand(minecraft.player.getUsedItemHand()).getItem());
                        KEY_USED_THIS_HOLD = true;
                        minecraft.player.connection.send(new ServerboundCustomPayloadPacket(new ServerboundUpdateMarkerPayload(marker)));
                    } else if (minecraft.options.keyUse.consumeClick() && minecraft.player.getItemInHand(minecraft.player.getUsedItemHand()).getItem() instanceof DyeItem dye) {
                        marker.setColour(DoomsMarkers.argbIntToFloatArray(dye.getDyeColor().getTextColor()));
                        KEY_USED_THIS_HOLD = true;
                        minecraft.player.connection.send(new ServerboundCustomPayloadPacket(new ServerboundUpdateMarkerPayload(marker)));
                    }
                }
            }

            var pose = context.pose();
            pose.pushMatrix();
            pose.translate(screenX - size / 2f, screenY - size / 2f);
            pose.scale(scale, scale);

            float alpha = focused ? 1.0f : 0.5f;

            if (marker.getIconIndex() == -1) {
                context.renderItem(marker.getItemIcon(), 0, 0);
            } else {
                ResourceLocation icon = DoomsMarkers.MARKER_ICONS.get(marker.getIconIndex());
                context.blit(icon, 0, 0, 0, 0, 16, 16, 16, 16);
            }

            Font font = minecraft.font;
            int textWidth = font.width(distanceText);
            pose.translate(8.0f - textWidth / 2.0f, 16.0f);
            context.drawString(font, distanceText, 0, 0, 0xFFFFFF);

            pose.popMatrix();
        }
    }

    public static void sendMarkerToServer(Marker marker) {
        if (Minecraft.getInstance().player == null) {
            return;
        }

        Minecraft.getInstance().player.connection.send(new ServerboundCustomPayloadPacket(new ServerboundAddMarkerPayload(marker)));
    }
}
