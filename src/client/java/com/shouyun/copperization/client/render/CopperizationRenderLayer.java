package com.shouyun.copperization.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class CopperizationRenderLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
	private static final double RELATIVE_Y_OFFSET = 128.0D;
	private static final double RELATIVE_Y_PRECISION = 256.0D;
	private static final float MAX_ENCODED_HEIGHT = 4.0F;
	private final LivingEntityRenderer<?, S, M> parentRenderer;

	public CopperizationRenderLayer(LivingEntityRenderer<?, S, M> parent) {
		super(parent);
		this.parentRenderer = parent;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		CopperRenderState copper = ((FabricRenderState) state).getData(CopperRenderStateExtractor.KEY);
		if (copper == null || copper.progress() <= 0.0F || state.isInvisible) return;

		int parameters = packParameters(state, copper);
		collector.order(8).submitModel(
			getParentModel(), state, poseStack, CopperRenderTypes.entity(parentRenderer.getTextureLocation(state)),
			light, LivingEntityRenderer.getOverlayCoords(state, 0.0F), parameters, null, state.outlineColor, null
		);
	}

	private static int packParameters(LivingEntityRenderState state, CopperRenderState copper) {
		double cameraY = Minecraft.getInstance().gameRenderer.mainCamera().position().y;
		double relativeY = Mth.clamp(state.y - cameraY + RELATIVE_Y_OFFSET, 0.0D, 255.996D);
		int encodedY = Mth.clamp((int)Math.round(relativeY * RELATIVE_Y_PRECISION), 0, 0xFFFF);
		int progress = Mth.clamp(Math.round(copper.progress() * 255.0F), 0, 255);
		int height = Mth.clamp(Math.round(Math.min(state.boundingBoxHeight, MAX_ENCODED_HEIGHT) / MAX_ENCODED_HEIGHT * 31.0F), 1, 31);
		int metadata = height << 3 | copper.oxidation().ordinal() << 1 | (copper.waxed() ? 1 : 0);
		return ARGB.color(metadata, progress, encodedY >>> 8, encodedY & 0xFF);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void register(LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderLayerRegistrationCallback.RegistrationHelper helper) {
		helper.register(new CopperizationRenderLayer((LivingEntityRenderer) renderer));
	}
}
