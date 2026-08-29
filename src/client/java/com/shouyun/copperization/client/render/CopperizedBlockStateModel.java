package com.shouyun.copperization.client.render;

import com.shouyun.copperization.block.CopperizedBlockData;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Delegates to the active resource-pack model and only tints quads at copperized positions. This
 * keeps cutout alpha, model shape, waterlogging and all original block-state behavior intact.
 */
public final class CopperizedBlockStateModel extends WrapperBlockStateModel {
	public CopperizedBlockStateModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		Predicate<@Nullable Direction> cullTest) {
		CopperizedBlockData data = data(level, pos);
		if (data == null) {
			wrapped.emitQuads(emitter, level, pos, state, random, cullTest);
			return;
		}
		emitter.pushTransform(quad -> {
			// Biome tint is applied after model emission and would otherwise replace the copper
			// colour on grass and leaves. Alpha remains supplied by the original texture.
			quad.tintIndex(-1);
			quad.multiplyColor(CopperizedBlockVisualProfile.tint(data));
			return true;
		});
		try {
			wrapped.emitQuads(emitter, level, pos, state, random, cullTest);
		} finally {
			emitter.popTransform();
		}
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		CopperizedBlockData data = data(level, pos);
		Object delegateKey = wrapped.createGeometryKey(level, pos, state, random);
		return data == null ? delegateKey : new GeometryKey(delegateKey, data.oxidationStage(), data.waxed());
	}

	private static CopperizedBlockData data(BlockAndTintGetter level, BlockPos pos) {
		return CopperizedClientChunkState.get(pos);
	}

	private record GeometryKey(@Nullable Object delegateKey, int oxidationStage, boolean waxed) {
	}
}
