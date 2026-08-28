package com.shouyun.copperization.mixin;

import com.shouyun.copperization.copper.CopperizationManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStatueMixin {
	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private void copperization$preventStatuePushing(CallbackInfoReturnable<Boolean> cir) {
		if (CopperizationManager.isStatue((LivingEntity) (Object) this)) cir.setReturnValue(false);
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void copperization$protectStatue(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
		if (CopperizationManager.isStatue((LivingEntity) (Object) this)) cir.setReturnValue(false);
	}
}
