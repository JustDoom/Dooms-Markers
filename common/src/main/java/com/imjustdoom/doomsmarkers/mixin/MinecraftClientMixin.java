package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.DoomsMarkersClient;
import com.imjustdoom.doomsmarkers.Marker;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @Unique
    private boolean doomsMarkers$markerDownLast = false;

    @Inject(at = @At("RETURN"), method = "tick")
    private void init(CallbackInfo info) {
        Minecraft minecraft = (Minecraft) (Object) this;

        if (minecraft.player == null) {
            return;
        }

        boolean currentDown = DoomsMarkersClient.MARKER_KEY_MAPPING.isDown();

        if (!this.doomsMarkers$markerDownLast && currentDown) {
            DoomsMarkersClient.KEY_USED_THIS_HOLD = false;
        }

        if (this.doomsMarkers$markerDownLast && !currentDown && !DoomsMarkersClient.KEY_USED_THIS_HOLD) {
            ItemStack itemStack = minecraft.player.getItemInHand(minecraft.player.getUsedItemHand());
            if (itemStack.getItem() == Items.FILLED_MAP) {
                MapItemSavedData data = MapItem.getSavedData(itemStack, minecraft.player.level());
                if (data == null) {
                    System.out.println("No data to fetch");
                    return;
                }

//                for (MapBanner banner : data.getBanners()) {
//                    DoomsMarkers.MARKERS.add(new Marker(new Vec3(banner.getPos().getX(), banner.getPos().getY() + 0.75f, banner.getPos().getZ()),
//                            DoomsMarkers.argbIntToFloatArray(banner.getColor().getTextColor()), 2));
//                }

                System.out.println("Map loc: " + data.centerX + " - " + data.centerZ);

//                for (MapDecoration decoration : data.getDecorations()) {
//                    System.out.println(decoration.getType());
//                    System.out.println(decoration.getX() + " - " + decoration.getY());
//                    DoomsMarkers.MARKERS.add(new Marker(new Vec3(decoration.getX() / 2f, 70, decoration.getY() / 2f),
//                            new float[]{1, 1, 1, 1}, 2));
//                }
            } else {
                Vec3 pos = minecraft.player.position();
                Marker marker = new Marker(new Vec3(pos.x, pos.y + 0.75f, pos.z), List.of(1f, 1f, 1f, 1f), 2);
                DoomsMarkersClient.MARKERS.add(marker);

                Tag encodedMarker = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker)
                        .getOrThrow(false, err -> System.err.println("Failed to encode marker: " + err));

                CompoundTag wrapper = new CompoundTag();
                wrapper.put("data", encodedMarker);

                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeNbt(wrapper);

                minecraft.player.connection.send(new ServerboundCustomPayloadPacket(DoomsMarkers.ADD_MARKER_PACKET, buf));
            }
        }

        this.doomsMarkers$markerDownLast = currentDown;
    }
}