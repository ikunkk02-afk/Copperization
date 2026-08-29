package com.shouyun.copperization.client.render;

import com.shouyun.copperization.block.CopperizedBlockData;
import com.shouyun.copperization.block.CopperizableBlockRegistry;
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
	private final @Nullable BlockState sourceState;

	public CopperizedBlockStateModel(BlockStateModel wrapped) {
		this(wrapped, null);
	}

	public CopperizedBlockStateModel(BlockStateModel wrapped, @Nullable BlockState sourceState) {
		super(wrapped);
		this.sourceState = sourceState;
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		Predicate<@Nullable Direction> cullTest) {
		CopperizedBlockData data = data(level, pos, state);
		BlockStateModel delegate = sourceState == null ? wrapped : CopperizedBlockModelPlugin.sourceModel(sourceState, wrapped);
		BlockState emittedState = sourceState == null ? state : sourceState;
		if (data == null) {
			delegate.emitQuads(emitter, level, pos, emittedState, random, cullTest);
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
			delegate.emitQuads(emitter, level, pos, emittedState, random, cullTest);
		} finally {
			emitter.popTransform();
		}
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		CopperizedBlockData data = data(level, pos, state);
		BlockStateModel delegate = sourceState == null ? wrapped : CopperizedBlockModelPlugin.sourceModel(sourceState, wrapped);
		Object delegateKey = delegate.createGeometryKey(level, pos, sourceState == null ? state : sourceState, random);
		return data == null ? delegateKey : new GeometryKey(delegateKey, data.oxidationStage(), data.waxed());
	}

	private static CopperizedBlockData data(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		CopperizedBlockData positional = CopperizedClientChunkState.get(pos);
		return positional != null ? positional : CopperizableBlockRegistry.physicalCopperData(state).orElse(null);
	}

	private record GeometryKey(@Nullable Object delegateKey, int oxidationStage, boolean waxed) {
	}
}
