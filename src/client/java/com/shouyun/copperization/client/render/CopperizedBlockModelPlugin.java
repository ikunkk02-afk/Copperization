package com.shouyun.copperization.client.render;

import com.shouyun.copperization.block.CopperizableBlockRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;

/** Registers once; Fabric rebuilds wrappers safely on every model/resource reload. */
public final class CopperizedBlockModelPlugin {
	private static final Map<BlockState, BlockStateModel> SOURCE_MODELS = new ConcurrentHashMap<>();
	private CopperizedBlockModelPlugin() {
	}

	public static void register() {
		ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake()
			.register(ModelModifier.WRAP_LAST_PHASE, (model, modelContext) -> {
				BlockState state = modelContext.state();
				var physical = CopperizableBlockRegistry.mappingForCopperized(state);
				if (physical.isPresent()) {
					BlockState sourceState = physical.get().originalBlock().withPropertiesOf(state);
					return new CopperizedBlockStateModel(model, sourceState);
				}
				SOURCE_MODELS.put(state, model);
				return CopperizableBlockRegistry.supportsPositionCopperization(state)
					? new CopperizedBlockStateModel(model) : model;
			}));
	}

	static BlockStateModel sourceModel(BlockState state, BlockStateModel fallback) {
		return SOURCE_MODELS.getOrDefault(state, fallback);
	}
}
