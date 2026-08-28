package com.shouyun.copperization.client.mixin;

import com.shouyun.copperization.client.render.CopperRenderStateExtractor;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererStateMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void copperization$extractCopperState(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		CopperRenderStateExtractor.extract(entity, state);
	}
}
