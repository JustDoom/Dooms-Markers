package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends LivingEntity {
    protected ServerPlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "die", at = @At(value = "TAIL"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        if (level().isClientSide()) {
            return;
        }

        Marker marker = new Marker(new Vec3(position().x, position().y + 0.75f, position().z), List.of(1f, 1f, 1f, 1f), 4);
        DoomsMarkers.MARKERS.get((ServerPlayer) (Object) this).add(marker);

        Tag encodedMarker = Marker.CODEC.encodeStart(NbtOps.INSTANCE, marker)
                .getOrThrow(false, err -> System.err.println("Failed to encode marker: " + err));

        CompoundTag wrapper = new CompoundTag();
        wrapper.put("data", encodedMarker);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeNbt(wrapper);

        ((ServerPlayer) (Object) this).connection.send(new ClientboundCustomPayloadPacket(DoomsMarkers.ADD_MARKER_PACKET, buf));
    }

    @Inject(at = @At("TAIL"), method = "addAdditionalSaveData")
    public void addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!DoomsMarkers.MARKERS.containsKey(player)) {
            return;
        }
        Tag encodedList = Marker.CODEC.listOf().encodeStart(NbtOps.INSTANCE, DoomsMarkers.MARKERS.get(player))
                .getOrThrow(false, err -> System.err.println("Save encode error: " + err));
        compoundTag.put("Markers", encodedList);
    }

    @Inject(at = @At("TAIL"), method = "readAdditionalSaveData")
    public void readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        System.out.println("LOADING DATA");
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!compoundTag.contains("Markers", Tag.TAG_LIST)) {
            DoomsMarkers.MARKERS.put(player, new ArrayList<>());
            System.out.println("Empty");
            return;
        }
        ListTag markersList = compoundTag.getList("Markers", Tag.TAG_COMPOUND);
        List<Marker> markers = Marker.CODEC.listOf().parse(NbtOps.INSTANCE, markersList)
                .getOrThrow(false, err -> System.err.println("Load parse error: " + err));
        System.out.println("Loaded " + markers.size() + " markers for " + player.getName().getString());
        System.out.println(markers);
        DoomsMarkers.MARKERS.put(player, new ArrayList<>(markers));
    }
}