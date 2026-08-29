package com.shouyun.copperization.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.function.Function;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import com.shouyun.copperization.Copperization;

public final class CopperRenderTypes {
	private static final RenderPipeline COPPERIZATION_ENTITY = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
			.withLocation(Copperization.id("pipeline/copperization_entity"))
			.withVertexShader(Copperization.id("core/copperization_entity"))
			.withFragmentShader(Copperization.id("core/copperization_entity"))
			.withShaderDefine("ALPHA_CUTOUT", 0.1F)
			.withShaderDefine("PER_FACE_LIGHTING")
			.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
			.withCull(false)
			.build()
	);

	private static final Function<Identifier, RenderType> ENTITY = Util.memoize(texture -> RenderType.create(
		"copperization_entity",
		RenderSetup.builder(COPPERIZATION_ENTITY)
			.withTexture("Sampler0", texture)
			.useLightmap()
			.useOverlay()
			.createRenderSetup()
	));

	private CopperRenderTypes() {
	}

	public static RenderType entity(Identifier originalTexture) {
		return ENTITY.apply(originalTexture);
	}
}
