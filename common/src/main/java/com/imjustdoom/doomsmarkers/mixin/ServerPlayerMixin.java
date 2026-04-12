package com.imjustdoom.doomsmarkers.mixin;

import com.imjustdoom.doomsmarkers.DoomsMarkers;
import com.imjustdoom.doomsmarkers.Marker;
import com.imjustdoom.doomsmarkers.ServerPlayerInterface;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

    @Inject(at = @At("TAIL"), method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V")
    public void addAdditionalSaveData(ValueOutput compoundTag, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (getMarkers().isEmpty()) {
            DoomsMarkers.LOG.info("No Markers exist for {} to save", player.getName().getString());
            return;
        }

        try {
            compoundTag.store("Markers", Marker.CODEC.listOf(), getMarkers());
            DoomsMarkers.LOG.info("Saved {} markers for {}", getMarkers().size(), player.getName().getString());
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to encode the Markers: {}", e.getMessage());
        }
    }

    @Inject(at = @At("TAIL"), method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V")
    public void readAdditionalSaveData(ValueInput compoundTag, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        try {
            var decoded = compoundTag.read("Markers", Marker.CODEC.listOf());
            if (decoded.isEmpty()) {
                DoomsMarkers.LOG.info("No Markers exist for {}", player.getName().getString());
                return;
            }

            getMarkers().addAll(decoded.get());
            DoomsMarkers.LOG.info("Loaded {} markers for {}", getMarkers().size(), player.getName().getString());
        } catch (Exception e) {
            DoomsMarkers.LOG.error("Unable to decode the Markers", e);
        }
    }

    @Override
    public List<Marker> getMarkers() {
        return this.doomsMarkers$markers;
    }
}