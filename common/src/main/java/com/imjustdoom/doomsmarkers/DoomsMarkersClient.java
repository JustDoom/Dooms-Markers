package com.imjustdoom.doomsmarkers;

import com.imjustdoom.doomsmarkers.util.ClientUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Handles the client side only part of the mod
 */
public class DoomsMarkersClient {
    public static final KeyMapping MARKER_KEY_MAPPING = new KeyMapping("category.doomsmarkers.use", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.doomsmarkers");
    public static final KeyMapping TOGGLE_MARKER_KEY_MAPPING = new KeyMapping("category.doomsmarkers.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.doomsmarkers");

    // List of focused markers. Used by other classes/mixins to check what marker is focused. Does support multiple in
    // focus. Maybe change it to just one
    public static Marker FOCUSED_MARKER;
    public static final List<Marker> MARKERS = new ArrayList<>();

    public static boolean KEY_USED_THIS_HOLD = false;
    public static boolean TOGGLED_MARKERS = true; // True displays them

    /**
     * Called every frame to render the UI layer of the mod.
     * It is done through a mixin and a weird way on Forge. Probably figure out how to do it properly
     * @param context
     */
    public static void renderMarkers(GuiGraphics context) {
        Minecraft minecraft = Minecraft.getInstance();

        // Make sure the player exists and is in a world
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        // Clear focused markers before checking for a toggle so that they won't ever be focused when toggled off
        FOCUSED_MARKER = null;

        if (!TOGGLED_MARKERS) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        ResourceKey<Level> currentDimension = minecraft.level.dimension();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vector3f cameraPos = camera.getPosition().toVector3f();

        Matrix4f modelView = new Matrix4f()
                .rotate(Axis.XP.rotationDegrees(camera.getXRot()))
                .rotate(Axis.YP.rotationDegrees(camera.getYRot() + 180.0f))
                .translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Get the projection matrix from fov without any fov modifier. Having it causes the sprinting state change look bad with marker positions
        Matrix4f projectionMatrix = minecraft.gameRenderer.getProjectionMatrix(minecraft.options.fov().get()); // * minecraft.player.getFieldOfViewModifier();

        Iterator<Marker> it = DoomsMarkersClient.MARKERS.iterator();
        while (it.hasNext()) {
            renderMarker(it, context, currentDimension, minecraft, modelView, projectionMatrix);
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderMarker(Iterator<Marker> it, GuiGraphics context, ResourceKey<Level> currentDimension, Minecraft minecraft, Matrix4f modelView, Matrix4f projectionMatrix) {
        final Marker marker = it.next();
        if (!marker.getDimension().equals(currentDimension)) {
            return;
        }

        Vector4d clipPos = new Vector4d(marker.getPosition().x, marker.getPosition().y, marker.getPosition().z, 1.0f);
        clipPos.mul(modelView);
        clipPos.mul(projectionMatrix);

        if (clipPos.w < 0.0f) {
            return;
        }

        float screenX = (float) ((context.guiWidth() / 2.0f) * (1.0f + clipPos.x / clipPos.w));
        float screenY = (float) ((context.guiHeight() / 2.0f) * (1.0f - clipPos.y / clipPos.w));

        double distance = Math.round(Math.sqrt(minecraft.player.distanceToSqr(marker.getPosition().x, marker.getPosition().y, marker.getPosition().z)));
        String distanceText = Config.get().compressUnit && distance >= 1000
                ? String.format("%.1fkm", distance / 1000f)
                : String.format("%dm", (int) distance);

        if (marker.getRemoveWhenNearby() != -1) {
            if (distance > marker.getRemoveWhenNearby()) {
                marker.setRemoveArmed(true);
            } else if (marker.isRemoveArmed()) {
                it.remove();
                ClientUtil.sendRemoveMarker(minecraft, marker);
                return;
            }
        }

        float scale = 1f;
        if (distance <= 10) {
            scale = 1f + (1f - (float) (distance / 10));
        }

        // Don't render if off screen
        float size = 16 * scale;
        if (screenX < -size || screenX > context.guiWidth() + size || screenY < -size || screenY > context.guiHeight() + size) {
            return;
        }

        // Check if the crosshair is over a marker
        float focusArea = 16 * scale / 1.5f;
        boolean focused = screenX > context.guiWidth() / 2f - focusArea && screenX < context.guiWidth() / 2f + focusArea
                && screenY > context.guiHeight() / 2f - focusArea && screenY < context.guiHeight() / 2f + focusArea;

        if (focused) {
            FOCUSED_MARKER = marker;
            if (MARKER_KEY_MAPPING.isDown()) {
                if (minecraft.options.keyAttack.consumeClick()) {
                    if (!marker.canPlayerRemove()) {
                        return;
                    }

                    KEY_USED_THIS_HOLD = true;

                    it.remove();
                    ClientUtil.sendRemoveMarker(minecraft, marker);
                } else if (minecraft.options.keyPickItem.consumeClick()) {
                    if (!marker.canPlayerCustomise()) {
                        return;
                    }

                    ClientUtil.sendUpdateIcon(minecraft, marker);
                } else if (minecraft.options.keyUse.consumeClick() && minecraft.player.getItemInHand(minecraft.player.getUsedItemHand()).getItem() instanceof DyeItem dye) {
                    if (!marker.canPlayerCustomise()) {
                        return;
                    }

                    ClientUtil.sendUpdateColour(minecraft, marker, dye);
                }
            }
        }

        PoseStack pose = context.pose();
        pose.pushPose();
        pose.translate(screenX - size / 2f, screenY - size / 2f, 0);
        pose.scale(scale, scale, 1f);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        if (marker.getIcon().equals(Markers.CUSTOM_ITEM)) {
            RenderSystem.setShaderColor(1, 1, 1, focused ? 1 : 0.5f);
            context.renderItem(marker.getItemIcon(), 0, 0);
            RenderSystem.setShaderColor(marker.getColour().get(0), marker.getColour().get(1), marker.getColour().get(2), focused ? 1 : 0.5f);
        } else {
            ResourceLocation icon = marker.getIcon().resourceLocation();
            RenderSystem.setShaderColor(marker.getColour().get(0), marker.getColour().get(1), marker.getColour().get(2), focused ? 1 : 0.5f);
            RenderSystem.setShaderTexture(0, icon);
            context.blit(icon, 0, 0, 0, 0, 16, 16, 16, 16);
        }

        Font font = minecraft.font;
        int textWidth = font.width(distanceText);
        pose.translate(8 - textWidth / 2.0, 16, 0);
        context.drawString(font, distanceText, 0, 0, 0xFFFFFF);

        pose.popPose();
    }
}
