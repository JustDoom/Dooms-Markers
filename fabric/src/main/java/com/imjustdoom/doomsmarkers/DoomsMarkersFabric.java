package com.imjustdoom.doomsmarkers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;

public class DoomsMarkersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            KeyBindingHelper.registerKeyBinding(DoomsMarkers.MARKER_KEY_MAPPING);
        }

        DoomsMarkers.init();

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                return;
            }

            Camera camera = minecraft.gameRenderer.getMainCamera();
            Vector3f cameraPos = camera.getPosition().toVector3f();

            Matrix4f modelView = new Matrix4f()
                    .rotate(Axis.XP.rotationDegrees(camera.getXRot()))
                    .rotate(Axis.YP.rotationDegrees(camera.getYRot() + 180.0f))
                    .translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            double fov = minecraft.options.fov().get() * minecraft.player.getFieldOfViewModifier();
            Matrix4f projectionMatrix = minecraft.gameRenderer.getProjectionMatrix(fov);

            DoomsMarkers.FOCUSED_MARKERS.clear();

            for (Marker marker : new ArrayList<>(DoomsMarkers.MARKERS)) {
                Vector4f clipPos = new Vector4f(marker.getPosition().getX(), marker.getPosition().getY(), marker.getPosition().getZ(), 1.0f);
                clipPos.mul(modelView);
                clipPos.mul(projectionMatrix);

                if (clipPos.w < 0.0f) {
                    continue;
                }

                float screenX = (context.guiWidth() / 2.0f) * (1.0f + clipPos.x / clipPos.w);
                float screenY = (context.guiHeight() / 2.0f) * (1.0f - clipPos.y / clipPos.w);

                // Don't render if off screen
                if (screenX < -8 || screenX > context.guiWidth() + 8 || screenY < -8 || screenY > context.guiHeight() + 8) {
                    continue;
                }

                // Check if the crosshair is over a marker
                boolean focused = screenX > context.guiWidth() / 2f - 12 && screenX < context.guiWidth() / 2f + 12
                        && screenY > context.guiHeight() / 2f - 12 && screenY < context.guiHeight() / 2f + 12;

                if (focused) {
                    DoomsMarkers.FOCUSED_MARKERS.add(marker);
                    if (DoomsMarkers.MARKER_KEY_MAPPING.isDown()) {
                        if (minecraft.options.keyAttack.consumeClick()) {
                            DoomsMarkers.MARKERS.remove(marker);
                        } else if (minecraft.options.keyPickItem.consumeClick()) {
                            marker.setIconIndex(-1);
                            marker.setItemIcon(minecraft.player.getItemInHand(minecraft.player.getUsedItemHand()).getItem());
                        }
                    }
                }

                double distance = Math.sqrt(minecraft.player.distanceToSqr(marker.getPosition().getX(), marker.getPosition().getY(), marker.getPosition().getZ()));
                String distanceText = String.format("%.0fm", distance);

                PoseStack pose = context.pose();
                pose.pushPose();
                pose.translate(screenX - 8, screenY - 8, 0);

                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                if (marker.getIconIndex() == -1) {
                    RenderSystem.setShaderColor(1, 1, 1, focused ? 1 : 1 / 2f);
                    context.renderItem(marker.getItemIcon(), 0, 0);
                    RenderSystem.setShaderColor(marker.getColour()[0], marker.getColour()[1], marker.getColour()[2], focused ? marker.getColour()[3] : marker.getColour()[3] / 2f);
                } else {
                    ResourceLocation icon = DoomsMarkers.MARKER_ICONS.get(marker.getIconIndex());
                    RenderSystem.setShaderColor(marker.getColour()[0], marker.getColour()[1], marker.getColour()[2], focused ? marker.getColour()[3] : marker.getColour()[3] / 2f);
                    RenderSystem.setShaderTexture(0, icon);
                    RenderSystem.enableBlend();
                    context.blit(icon, 0, 0, 0, 0, 16, 16, 16, 16);
                }

                Font font = minecraft.font;
                int textWidth = font.width(distanceText);
                pose.translate(8 - textWidth / 2.0, 16, 0);
                context.drawString(font, distanceText, 0, 0, 0xFFFFFF);

                RenderSystem.disableBlend();
                pose.popPose();
            }

            RenderSystem.setShaderColor(1, 1, 1, 1);
        });


    }
}
