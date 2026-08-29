package com.shouyun.copperization.mixin;

import com.shouyun.copperization.copper.CopperizationManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityStatueFireMixin {
	@Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
	private void copperization$preventStatueIgnition(int remainingTicks, CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;
		if (remainingTicks > 0 && entity instanceof LivingEntity living && CopperizationManager.isStatue(living)) {
			CopperizationManager.clearStatueFire(living);
			ci.cancel();
		}
	}
}
