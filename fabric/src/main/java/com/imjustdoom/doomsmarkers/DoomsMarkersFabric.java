package com.imjustdoom.doomsmarkers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class DoomsMarkersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DoomsMarkers.init();

        ResourceLocation icon = new ResourceLocation("doomsmarkers", "textures/marker.png");
        List<Marker> markers = new ArrayList<>();
        markers.add(new Marker(new BlockPos(0, 70, 0), new float[]{1, 0.75f, 0.2f, 1}));
        markers.add(new Marker(new BlockPos(100, 70, 25), new float[]{0.9f, 0.35f, 0.72f, 1}));
        markers.add(new Marker(new BlockPos(-50, 30, -20), new float[]{0.24f, 0.5f, 0.298f, 1}));

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

            Matrix4f projectionMatrix = minecraft.gameRenderer.getProjectionMatrix(minecraft.options.fov().get());

            for (Marker marker : markers) {
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

                PoseStack pose = context.pose();
                pose.pushPose();
                pose.translate(screenX - 8, screenY - 8, 0);

                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(marker.colour()[0], marker.colour()[1], marker.colour()[2], focused ? marker.colour()[3] : marker.colour()[3] / 2f);
                RenderSystem.setShaderTexture(0, icon);
                RenderSystem.enableBlend();
                context.blit(icon, 0, 0, 0, 0, 16, 16, 16, 16);

                RenderSystem.disableBlend();
                pose.popPose();
            }

            RenderSystem.setShaderColor(1, 1, 1, 1);
        });
    }
}
