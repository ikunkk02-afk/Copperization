package com.shouyun.copperization.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.level.block.WeatheringCopper;

public final class CopperizationRenderLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
	public CopperizationRenderLayer(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		CopperRenderState copper = ((FabricRenderState) state).getData(CopperRenderStateExtractor.KEY);
		if (copper == null || copper.progress() <= 0.0F || state.isInvisible) return;
		int color = color(copper.oxidation(), copper.waxed());
		renderColoredCutoutModel(getParentModel(), CopperMask.forProgress(copper.progress()), poseStack, collector, light, state, color, 8);
	}

	private static int color(WeatheringCopper.WeatherState oxidation, boolean waxed) {
		int base = switch (oxidation) {
			case UNAFFECTED -> 0xFFD47C45;
			case EXPOSED -> 0xFFB86E53;
			case WEATHERED -> 0xFF72977B;
			case OXIDIZED -> 0xFF4FA88C;
		};
		if (!waxed) return base;
		return switch (oxidation) {
			case UNAFFECTED -> 0xFFE38E57;
			case EXPOSED -> 0xFFC47B61;
			case WEATHERED -> 0xFF82A68A;
			case OXIDIZED -> 0xFF62B89D;
		};
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void register(LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderLayerRegistrationCallback.RegistrationHelper helper) {
		helper.register(new CopperizationRenderLayer((RenderLayerParent) renderer));
	}
}
