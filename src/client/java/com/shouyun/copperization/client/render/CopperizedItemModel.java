package com.shouyun.copperization.client.render;

import com.shouyun.copperization.block.CopperizedBlockData;
import com.shouyun.copperization.client.mixin.ItemStackRenderStateAccessor;
import com.shouyun.copperization.client.mixin.LayerRenderStateAccessor;
import java.util.ListIterator;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBakedItemModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Applies the same corrosion palette to creative-tab and inventory variants without copying textures. */
public final class CopperizedItemModel extends WrapperBakedItemModel {
	private final int tint;

	public CopperizedItemModel(ItemModel wrapped, CopperizedBlockData data) {
		super(wrapped);
		this.tint = CopperizedBlockVisualProfile.tint(data);
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
		ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner itemOwner, int seed) {
		super.update(state, stack, resolver, displayContext, level, itemOwner, seed);
		ItemStackRenderStateAccessor stateAccessor = (ItemStackRenderStateAccessor) (Object) state;
		ItemStackRenderState.LayerRenderState[] layers = stateAccessor.copperization$layers();
		for (int i = 0; i < stateAccessor.copperization$activeLayerCount(); i++) tintLayer(layers[i]);
	}

	private void tintLayer(ItemStackRenderState.LayerRenderState layer) {
		var colors = layer.tintLayers();
		colors.clear();
		colors.add(tint);
		ListIterator<BakedQuad> iterator = ((LayerRenderStateAccessor) (Object) layer).copperization$quads().listIterator();
		while (iterator.hasNext()) {
			BakedQuad quad = iterator.next();
			BakedQuad.MaterialInfo material = quad.materialInfo();
			if (material.tintIndex() == 0) continue;
			BakedQuad.MaterialInfo tinted = new BakedQuad.MaterialInfo(material.sprite(), material.layer(),
				material.itemRenderType(), 0, material.shade(), material.lightEmission());
			iterator.set(new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), tinted));
		}
	}
}
