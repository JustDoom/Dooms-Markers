package com.imjustdoom.doomsmarkers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class DoomsMarkersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
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

            for (Marker marker : DoomsMarkers.MARKERS) {
                Vector4f clipPos = new Vector4f(marker.position().getX(), marker.position().getY(), marker.position().getZ(), 1.0f);
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

                double distance = Math.sqrt(minecraft.player.distanceToSqr(marker.position().getX(), marker.position().getY(), marker.position().getZ()));
                String distanceText = String.format("%.0fm", distance);

                PoseStack pose = context.pose();
                pose.pushPose();
                pose.translate(screenX - 8, screenY - 8, 0);

                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(marker.colour()[0], marker.colour()[1], marker.colour()[2], focused ? marker.colour()[3] : marker.colour()[3] / 2f);
                RenderSystem.setShaderTexture(0, marker.icon());
                RenderSystem.enableBlend();
                context.blit(marker.icon(), 0, 0, 0, 0, 16, 16, 16, 16);

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
