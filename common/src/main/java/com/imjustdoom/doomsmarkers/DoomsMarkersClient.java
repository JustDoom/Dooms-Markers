package com.imjustdoom.doomsmarkers;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeItem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class DoomsMarkersClient {
    public static final KeyMapping MARKER_KEY_MAPPING = new KeyMapping("category.doomsmarkers.use", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.doomsmarkers");
    public static final List<Marker> FOCUSED_MARKERS = new ArrayList<>();
    public static final List<Marker> MARKERS = new ArrayList<>();

    public static boolean KEY_USED_THIS_HOLD = false;

    public static void renderMarkers(GuiGraphics context) {
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

        FOCUSED_MARKERS.clear();

        for (Marker marker : new ArrayList<>(DoomsMarkersClient.MARKERS)) {
            Vector4d clipPos = new Vector4d(marker.getPosition().x, marker.getPosition().y, marker.getPosition().z, 1.0f);
            clipPos.mul(modelView);
            clipPos.mul(projectionMatrix);

            if (clipPos.w < 0.0f) {
                continue;
            }

            float screenX = (float) ((context.guiWidth() / 2.0f) * (1.0f + clipPos.x / clipPos.w));
            float screenY = (float) ((context.guiHeight() / 2.0f) * (1.0f - clipPos.y / clipPos.w));

            // Don't render if off screen
            if (screenX < -8 || screenX > context.guiWidth() + 8 || screenY < -8 || screenY > context.guiHeight() + 8) {
                continue;
            }

            // Check if the crosshair is over a marker
            boolean focused = screenX > context.guiWidth() / 2f - 12 && screenX < context.guiWidth() / 2f + 12
                    && screenY > context.guiHeight() / 2f - 12 && screenY < context.guiHeight() / 2f + 12;

            if (focused) {
                FOCUSED_MARKERS.add(marker);
                if (MARKER_KEY_MAPPING.isDown()) {
                    if (minecraft.options.keyAttack.consumeClick()) {
                        DoomsMarkersClient.MARKERS.remove(marker);
                        KEY_USED_THIS_HOLD = true;

                        CompoundTag wrapper = new CompoundTag();
                        wrapper.putString("uuid", marker.getUuid().toString());

                        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                        buf.writeNbt(wrapper);
                        minecraft.player.connection.send(new ServerboundCustomPayloadPacket(DoomsMarkers.DELETE_MARKER_PACKET, buf));
                    } else if (minecraft.options.keyPickItem.consumeClick()) {
                        marker.setIconIndex(-1);
                        marker.setItemIcon(minecraft.player.getItemInHand(minecraft.player.getUsedItemHand()).getItem());
                        KEY_USED_THIS_HOLD = true;

                        Tag encoded = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker)
                                .getOrThrow(false, err -> System.err.println("Failed to encode markers: " + err));

                        CompoundTag wrapper = new CompoundTag();
                        wrapper.put("data", encoded);

                        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                        buf.writeNbt(wrapper);
                        minecraft.player.connection.send(new ServerboundCustomPayloadPacket(DoomsMarkers.UPDATE_MARKER_PACKET, buf));
                    } else if (minecraft.options.keyUse.consumeClick() && minecraft.player.getItemInHand(minecraft.player.getUsedItemHand()).getItem() instanceof DyeItem dye) {
                        marker.setColour(DoomsMarkers.argbIntToFloatArray(dye.getDyeColor().getTextColor()));
                        KEY_USED_THIS_HOLD = true;

                        Tag encoded = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker)
                                .getOrThrow(false, err -> System.err.println("Failed to encode markers: " + err));

                        CompoundTag wrapper = new CompoundTag();
                        wrapper.put("data", encoded);

                        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                        buf.writeNbt(wrapper);
                        minecraft.player.connection.send(new ServerboundCustomPayloadPacket(DoomsMarkers.UPDATE_MARKER_PACKET, buf));
                    }
                }
            }

            double distance = Math.sqrt(minecraft.player.distanceToSqr(marker.getPosition().x, marker.getPosition().y, marker.getPosition().z));
            String distanceText = String.format("%.0fm", distance);

            PoseStack pose = context.pose();
            pose.pushPose();
            pose.translate(screenX - 8, screenY - 8, 0);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            if (marker.getIconIndex() == -1) {
                RenderSystem.setShaderColor(1, 1, 1, focused ? 1 : 1 / 2f);
                context.renderItem(marker.getItemIcon(), 0, 0);
                RenderSystem.setShaderColor(marker.getColour().get(0), marker.getColour().get(1), marker.getColour().get(2), focused ? 1 : 0.5f);
            } else {
                ResourceLocation icon = DoomsMarkers.MARKER_ICONS.get(marker.getIconIndex());
                RenderSystem.setShaderColor(marker.getColour().get(0), marker.getColour().get(1), marker.getColour().get(2), focused ? 1 : 0.5f);
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
    }
}
