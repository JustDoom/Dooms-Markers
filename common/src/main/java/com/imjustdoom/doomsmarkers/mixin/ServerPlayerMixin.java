package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.ServerPlayerInterface;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends LivingEntity implements ServerPlayerInterface {
    @Unique
    public final List<Marker> doomsMarkers$markers = new ArrayList<>();

    protected ServerPlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "die", at = @At(value = "TAIL"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        if (level().isClientSide()) {
            return;
        }

        Marker marker = new Marker(new Vec3(position().x, position().y + 0.75f, position().z), List.of(1f, 1f, 1f, 1f), 4);
        getMarkers().add(marker);

        DoomsMarkers.sendMarkerToPlayer((ServerPlayer) (Object) this, marker);
    }

    @Inject(at = @At("TAIL"), method = "addAdditionalSaveData")
    public void addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (getMarkers().isEmpty()) {
            DoomsMarkers.LOG.info("No Markers exist for {} to save", player.getName().getString());
            return;
        }

        try {
            Tag encodedList = Marker.CODEC.listOf().encodeStart(NbtOps.INSTANCE, getMarkers()).getOrThrow(false, null);
            compoundTag.put("Markers", encodedList);
            DoomsMarkers.LOG.info("Saved {} markers for {}", getMarkers().size(), player.getName().getString());
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to encode the Markers: {}", e.getMessage());
        }
    }

    @Inject(at = @At("TAIL"), method = "readAdditionalSaveData")
    public void readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!compoundTag.contains("Markers", Tag.TAG_LIST)) {
            DoomsMarkers.LOG.info("No Markers exist for {}", player.getName().getString());
            return;
        }

        try {
            ListTag markersList = compoundTag.getList("Markers", Tag.TAG_COMPOUND);
            getMarkers().addAll(Marker.CODEC.listOf().parse(NbtOps.INSTANCE, markersList).getOrThrow(false, null));
            DoomsMarkers.LOG.info("Loaded {} markers for {}", getMarkers().size(), player.getName().getString());
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to encode the Markers: {}", e.getMessage());
        }
    }

    @Override
    public List<Marker> getMarkers() {
        return this.doomsMarkers$markers;
    }
}