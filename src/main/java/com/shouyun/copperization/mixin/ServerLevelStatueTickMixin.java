package com.shouyun.copperization.mixin;

import com.shouyun.copperization.copper.CopperOxidationManager;
import com.shouyun.copperization.copper.CopperizationManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelStatueTickMixin {
	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void copperization$freezeStatueTick(Entity entity, CallbackInfo ci) {
		if (entity instanceof LivingEntity living && CopperizationManager.isStatue(living)) {
			ServerLevel level = (ServerLevel) (Object) this;
			CopperOxidationManager.tickStatue(level, living);
			CopperizationManager.freeze(living, CopperizationManager.getState(living));
			ci.cancel();
		}
	}
}
